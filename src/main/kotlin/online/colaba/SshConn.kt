package online.colaba

import java.io.File
import java.io.IOException

/**
 * SSH transport over the SYSTEM OpenSSH (ssh/scp) and, when both sides have it, rsync.
 * Modern OpenSSH accepts any key (ed25519, openssh format), not only PEM RSA.
 * ControlMaster reuses one TCP + handshake for every command/copy (parity with the old single jsch session);
 * rsync rides the same master socket through `-e`.
 */
class SshConn(
    private val host: String,
    private val user: String,
    private val key: File,
    checkKnownHosts: Boolean = false,
    /** `false` keeps every copy on scp even where rsync exists: the escape hatch if rsync misbehaves somewhere. */
    private val useRsync: Boolean = true,
) : AutoCloseable {

    private val ctlPath: String = File.createTempFile("colaba-ssh-", ".ctl").apply { delete() }.path

    private val opts: List<String> = buildList {
        add("-i"); add(key.path)
        add("-o"); add("BatchMode=yes")
        add("-o"); add("ControlMaster=auto")
        add("-o"); add("ControlPath=$ctlPath")
        add("-o"); add("ControlPersist=120")
        if (!checkKnownHosts) {
            add("-o"); add("StrictHostKeyChecking=no")
            add("-o"); add("UserKnownHostsFile=/dev/null")
        }
    }

    /** `rsync -e` takes the remote shell as ONE string and splits it on spaces itself, honouring quotes. */
    private val rsyncShell: String = (listOf("ssh") + opts).joinToString(" ") { if (it.contains(' ')) "'$it'" else it }

    /**
     * rsync on both ends. Probed once per connection; without it every copy falls back to scp,
     * which moves the same bytes, just all of them and one file per round trip.
     */
    val rsyncAvailable: Boolean by lazy {
        if (!useRsync) return@lazy false
        val local = try {
            ProcessBuilder(cmdPrefix + listOf("rsync", "--version")).redirectErrorStream(true).start()
                .run { inputStream.readAllBytes(); waitFor() == 0 }
        } catch (e: IOException) {
            false
        }
        val remote = local && execute("command -v rsync >/dev/null 2>&1 && echo yes || echo no") == "yes"
        if (!remote) println("ℹ️ rsync is missing ${if (local) "on the remote host" else "locally"}: copying with scp")
        remote
    }

    /** Remote command. Returns trimmed stdout. Throws on non-zero exit (parity with groovy-ssh execute). */
    fun execute(command: String): String {
        val proc = ProcessBuilder(listOf("ssh") + opts + listOf("$user@$host", command)).start()
        val out = proc.inputStream.bufferedReader().readText()
        val err = proc.errorStream.bufferedReader().readText()
        val code = proc.waitFor()
        if (code != 0) throw RuntimeException("ssh exit=$code for [$command]:\n$err")
        return out.trim()
    }

    /** Upload a file/dir into the already-created remote directory `into`. Recursive. */
    fun upload(from: File, into: String) {
        val proc = ProcessBuilder(listOf("scp") + opts + listOf("-r", from.path, "$user@$host:$into")).start()
        val err = proc.errorStream.bufferedReader().readText()
        val code = proc.waitFor()
        if (code != 0) throw RuntimeException("scp exit=$code [${from.path}] -> [$into]:\n$err")
    }

    /**
     * Make the remote copy of [from] equal to the local one inside the existing remote directory [into]:
     * a folder is mirrored (`--delete` drops what is gone locally), a file is overwritten.
     *
     * Only changed blocks travel: a rebuilt fat jar shares almost all of its bytes with the previous one
     * (the nested dependency jars are stored as is), so a 140 MB jar costs a few MB on the wire. Every file
     * lands through a temp file and a rename, so an interrupted run leaves the previous copy intact instead
     * of the empty folder that `rm -rf` + scp left behind. Owner and group are not carried over: the files
     * belong to the remote user, as they do after scp. Returns rsync's `--stats` lines about the transfer.
     */
    fun mirror(from: File, into: String): List<String> {
        val target = into.trimEnd('/')
        val args = if (from.isDirectory) listOf("--delete", "${from.path.trimEnd('/')}/", "$user@$host:$target/${from.name}/")
        else listOf(from.path, "$user@$host:$target/")
        return rsync(listOf("-rlpt", "--stats") + args)
            .filter { it.startsWith("Total file size") || it.startsWith("Literal data") || it.startsWith("Matched data") }
    }

    /**
     * Copy [paths] (relative to [root]; files and folders) into the remote directory [into] keeping their
     * relative layout, in ONE rsync call. Folders merge with what is there, as `scp -r` does. One call
     * instead of a channel, a `mkdir` and an scp handshake per file: over a transatlantic link that is a
     * second or two saved on every file.
     */
    fun uploadRelative(root: File, paths: List<String>, into: String) {
        if (paths.isEmpty()) return
        val list = File.createTempFile("colaba-rsync-", ".list")
        try {
            list.writeText(paths.joinToString(separator = "\n", postfix = "\n"))
            rsync(listOf("-rlptR", "--files-from=${list.path}", "${root.path.trimEnd('/')}/", "$user@$host:${into.trimEnd('/')}/"))
        } finally {
            list.delete()
        }
    }

    private fun rsync(args: List<String>): List<String> {
        val proc = ProcessBuilder(cmdPrefix + listOf("rsync", "-e", rsyncShell) + args).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readLines()
        val code = proc.waitFor()
        if (code != 0) throw RuntimeException("rsync exit=$code ${args.takeLast(2)}:\n${out.takeLast(20).joinToString("\n")}")
        return out
    }

    override fun close() {
        // tear down the master socket (best-effort)
        runCatching { ProcessBuilder(listOf("ssh") + opts + listOf("-O", "exit", "$user@$host")).start().waitFor() }
        runCatching { File(ctlPath).delete() }
    }
}

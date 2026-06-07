package online.colaba

import java.io.File

/**
 * SSH transport over the SYSTEM OpenSSH (ssh/scp)
 * Modern OpenSSH accepts any key (ed25519, openssh format), not only PEM RSA.
 * ControlMaster reuses one TCP + handshake for every command/copy (parity with the old single jsch session).
 */
class SshConn(
    private val host: String,
    private val user: String,
    private val key: File,
    checkKnownHosts: Boolean = false,
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

    override fun close() {
        // tear down the master socket (best-effort)
        runCatching { ProcessBuilder(listOf("ssh") + opts + listOf("-O", "exit", "$user@$host")).start().waitFor() }
        runCatching { File(ctlPath).delete() }
    }
}

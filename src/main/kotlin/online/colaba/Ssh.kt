package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler
import java.io.File

const val sshGroup = "ssh"

open class Ssh : Executor() {
    init {
        group = sshGroup
        description = "Publish by FTP your distribution with SSH commands"
    }

    @get:Input
    @Optional
    var host: String? = null
    @get:Input
    @Optional
    var user: String? = null

    @get:Input
    @Optional
    var server: SshServer? = null

    @get:Input
    var frontendFolder: String = frontendService
    @get:Input
    var backendFolder: String = backendService

    @get:Input
    var checkKnownHosts: Boolean = false

    @get:Input
    @Optional
    var directory: String? = null

    @get:Input
    @Optional
    var run: String? = null

    @get:Input
    var frontend: Boolean = false
    @get:Input
    var backend: Boolean = false
    @get:Input
    var docker: Boolean = false
    @get:Input
    var gradle: Boolean = false
    @get:Input
    var nginx: Boolean = false

    @TaskAction
    fun run() {
        Ssh.newService().runSessions {
            session(remote()) {
                if (frontend) copyFolderWithOverride(frontendFolder)
                if (nginx) copyFolderWithOverride(nginxService)
                if (backend) copyFolderWithOverride(SshServer.backendDistFolder)
                if (docker) copyFromRootAndEachSubFolder(dockerComposeFile, dockerfile, dockerignoreFile, ".env")
                if (gradle) copyGradle()

                directory?.let { copyFolderWithOverride(it) }

                run?.let {
                    println("\n\uD83D\uDD11 Executing command on remote server: { $run }")
                    println(
                        execute(it)
                    )
                }
            }
        }
    }

    private fun remote() =
        (server ?: if (host != null) SshServer(hostSsh = host!!, userSsh = user!!) else SshServer()).remote(
            checkKnownHosts
        )

    private fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))
    private fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
        session(*remotes, delegateClosureOf(action))

    private fun SessionHandler.put(from: Any, into: String) = put(hashMapOf("from" to from, "into" to into))

    private fun SessionHandler.remoteIsExist(into: String) =
        execute("test -d ${project.name}/$into && echo true || echo false")?.toBoolean() ?: false

    private fun SessionHandler.remoteMkDir(into: String) = into.apply { execute("mkdir --parent $this") }
    private fun SessionHandler.remoteRm(vararg folders: String) = folders.iterator().forEach { println(">✂️ Removing remote folder [$it]"); execute("rm -fr $it") }

    private fun SessionHandler.copyFromRootAndEachSubFolder(vararg files: String) {
        files.iterator().forEach { file -> copy(file); copyBack(file); copyFront(file) }
    }

    private fun SessionHandler.copyFront(vararg files: String) = copy(files, frontendFolder)
    private fun SessionHandler.copyBack(vararg files: String) = copy(files, backendFolder)

    private fun SessionHandler.copyGradle() {
        val buildFile = ifNotGroovyThenKotlin("build.gradle")
        copyFolderIfNotRemote("gradle")
        copy(buildFile, ifNotGroovyThenKotlin("settings.gradle"), "gradlew", "gradlew.bat")
        copyBack(buildFile)
        copyFront(buildFile)
        execute("chmod +x ${project.name}/gradlew")

        val buildSrc = "buildSrc"
        "$buildSrc/build".removeLocal()
        copyFolderWithOverride(buildSrc)
    }

    private fun ifNotGroovyThenKotlin(buildFile: String): String =
        if (File(buildFile).exists()) buildFile else "$buildFile.kts"

    private fun String.removeLocal() {
        File("${project.rootDir}/$this".normalizeForWindows()).apply { if (exists()) deleteRecursively() }
    }

    private fun SessionHandler.copyFolderIfNotRemote(directory: String = "") =
        if (!remoteIsExist("${project.name}/$directory")) copyFolderWithOverride(directory) else false

    private fun SessionHandler.copyFolderWithOverride(directory: String = ""): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        if (!File(fromLocalPath).exists()) return false
        println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] \n\t  to remote {$toRemote}\n")
        remoteRm(toRemote)
        val toRemoteParent = File(toRemote).parent.normalizeForWindows()
        println("> \uD83D\uDDC3️ Copy [${fromLocalPath.substringAfterLast('/')}] into remote {$toRemoteParent} in progress...")
        put(File(fromLocalPath), remoteMkDir(toRemoteParent))
        return true
    }


    private fun SessionHandler.copy(vararg files: String) = copy(files)
    private fun SessionHandler.copy(files: Array<out String> = arrayOf(""), remote: String = ""): Boolean {
        var exist = 0; files.iterator().forEach { if (copy(it, remote)) exist++ }; return exist > 0
    }

    private fun SessionHandler.copy(file: String, remote: String) = copy(File(file), remote)
    private fun SessionHandler.copy(file: File, remote: String = project.name): Boolean {
        val from = File("${project.rootDir}/$remote/$file".normalizeForWindows())
        val into = "${project.name}/$remote"
        if (from.exists()) {
            put(from, remoteMkDir(into))
            println("\uD83D\uDDA5️ FILE from local [$from] \n\t to remote {$into} - DONE")
            return true
        } else println("\t☣️ > Skip not found: $from\n")
        return false
    }

}


fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)

val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup)

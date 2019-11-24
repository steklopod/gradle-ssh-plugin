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

    //TODO
    @get:Input
    @Optional
    var host: String? = null
    //TODO
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
    var static: Boolean = false
    @get:Input
    var docker: Boolean = false
    @get:Input
    var gradle: Boolean = false
    @get:Input
    var nginx: Boolean = false

    @TaskAction
    fun run() {
        newService().runSessions {
            session(remote()) {
                if (frontend) copyFolder(frontendFolder)
                if (nginx) copyFolder(nginxService)
                if (backend) copyFolder(SshServer.backendDistFolder)
                if (static) copyStatic()
                if (docker) copyInEach(dockerComposeFile, dockerfile, dockerignoreFile, ".env")
                if (gradle) copyGradle()

                directory?.let { copyFolder(it) }

                run?.let {
                    println("\n\uD83D\uDD11 Executing command on remote server: { $run }")
                    println(
                            execute("$it")
                    )

                }
            }
        }
    }

    private fun SessionHandler.copyInEach(vararg files: String) {
        files.iterator().forEach { file -> copy(file); copyBack(file); copyFront(file) }
    }

    //TODO - user, host : check
    private fun remote() = (server ?: SshServer()).remote(checkKnownHosts)

    private fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))
    private fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) = session(*remotes, delegateClosureOf(action))
    private fun SessionHandler.put(from: Any, into: String) = put(hashMapOf("from" to from, "into" to into))

    private fun SessionHandler.isRemoteExists(into: String) = execute("test -d ${project.name}/$into && echo true || echo false").toBoolean()
    private fun SessionHandler.toRemoteFolder(into: String): String {
        execute("mkdir --parent $into"); return into
    }

    private fun SessionHandler.removeRemote(vararg folders: String) {
        folders.iterator().forEach { execute("rm -fr $it") }
    }

    private fun SessionHandler.copyFront(vararg files: String) = copy(files, frontendFolder)
    private fun SessionHandler.copyBack(vararg files: String) = copy(files, backendFolder)
    private fun SessionHandler.copyStatic(staticFolder: String = "static"): Boolean {
        val isExistNow = isRemoteExists(staticFolder) || isRemoteExists("$backendFolder/$staticFolder")
        return !isExistNow && !copy(staticFolder) && !copy(staticFolder, backendFolder)
    }

    private fun SessionHandler.copyGradle() {
        val buildSrc = "buildSrc"
        val buildFile = "build.gradle.kts"
        copyFolderIfNotRemote("gradle")
        copy(buildFile, "settings.gradle.kts", "gradlew", "gradlew.bat")
        execute("chmod +x ${project.name}/gradlew")

        "$buildSrc/build".removeLocal()
        copyFolder(buildSrc)
        copyBack(buildFile)
        copyFront(buildFile)
    }

    private fun String.removeLocal() {
        File("${project.rootDir}/$this").apply { if (exists()) deleteRecursively() }
    }

    private fun SessionHandler.copyFolderIfNotRemote(directory: String = "") =
            if (!isRemoteExists("${project.name}/$directory")) copyFolder(directory) else false

    private fun SessionHandler.copyFolder(directory: String = ""): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] \n\t  to remote {$toRemote}\n")
        removeRemote(toRemote)
        if (!File(fromLocalPath).exists()) return false
        put(File(fromLocalPath), toRemoteFolder(File(toRemote).parent))
        return true
    }

    private fun SessionHandler.copy(vararg files: String) = copy(files)
    private fun SessionHandler.copy(files: Array<out String> = arrayOf(project.name), remote: String = ""): Boolean {
        var exist = 0; files.iterator().forEach { if (copy(it, remote)) exist++ }
        return exist > 0
    }

    private fun SessionHandler.copy(file: String, remote: String) = copy(File(file), remote)
    private fun SessionHandler.copy(file: File, remote: String = project.name): Boolean {
        val from = File("${project.rootDir}/$remote/$file".normalizeForWindows())
        val into = "${project.name}/$remote"
        if (from.exists()) {
            put(from, toRemoteFolder(into))
            println("\uD83D\uDDA5️ FILE from local [$from] \n\t to remote {$into}")
            return true
        } else println("\t☣️ > Skip not found: $from\n")
        return false
    }

    private fun newService(): Service {
        return Ssh.newService()
    }
}


fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)

val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup)

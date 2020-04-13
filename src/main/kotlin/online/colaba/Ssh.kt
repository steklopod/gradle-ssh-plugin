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
import kotlin.concurrent.thread

const val sshGroup = "ssh"

open class Ssh : Cmd() {
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
    var clearNuxt: Boolean = false

    @get:Input
    var postgres: Boolean = false

    @get:Input
    var monolit: Boolean = false

    @get:Input
    var admin: Boolean = false

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
        Ssh.newService().runSessions {
            session(remote()) {
                if (clearNuxt) deleteNodeModulesAndNuxtFolders()
                if (monolit) backendServices = setOf(backendFolder)
                if (admin) backendServices += adminServer

                if (postgres) copyIfNotRemote(postgresService)
                if (static) copyIfNotRemote(staticDir)

                if (nginx) copyWithOverride(nginxService)

                if (frontend) thread { copyWithOverride(frontendFolder) }

                if (backend) backendServices.parallelStream().forEach { copyWithOverride(jarLibsFolder(it)) }

                if (gradle) copyGradle()

                if (docker) copyInEach("docker-compose.yml", "Dockerfile", ".dockerignore", ".env")

                directory?.let { copyWithOverride(it) }

                run?.let {
                    println("\n\uD83D\uDD11 Executing command on remote server: { $run }")
                    println(execute(it))
                }
            }
        }
    }

    private fun SessionHandler.copyInEach(vararg files: String) = files.forEach { file ->
        copy(file); copyFront(file); copyBack(file); copyPostgres(file)
    }

    private fun SessionHandler.copyGradle() {
        copyGradleWrapperIfNotExists()
        val buildFile = ifNotGroovyThenKotlin("build.gradle")
        val settingsFile = ifNotGroovyThenKotlin("settings.gradle")
        copy(settingsFile)
        copy(buildFile)
        copy("gradle.properties")
        copyFront(buildFile)

        copyBack(buildFile)
        copyBack(settingsFile)

        val buildSrc = "buildSrc"
        "$buildSrc/build".removeLocal()
        copyWithOverride(buildSrc)
    }

    private fun SessionHandler.copyGradleWrapperIfNotExists() {
        if (copyIfNotRemote("gradle")) {
            copy("gradlew")
            copy("gradlew.bat")
            execute("chmod +x ${project.name}/gradlew")
        }
    }

    private fun SessionHandler.copyIfNotRemote(directory: String = "") =
        if (!remoteIsExist(directory)) copyWithOverride(directory) else false

    private fun SessionHandler.copyWithOverride(directory: String = ""): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        if (!File(fromLocalPath).exists())
            return false
        println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] \n\t  to remote {$toRemote}\n")
        remoteRm(toRemote)
        val toRemoteParent = File(toRemote).parent.normalizeForWindows()
        println("> \uD83D\uDDC3️ Copy [${fromLocalPath.substringAfterLast('/')}] into remote {$toRemoteParent} in progress...\n")
        put(File(fromLocalPath), remoteMkDir(toRemoteParent))
        return true
    }

    private fun SessionHandler.put(from: Any, into: String) = put(hashMapOf("from" to from, "into" to into))

    private fun SessionHandler.remoteIsExist(into: String): Boolean {
        val exists = execute("test -d ${project.name}/$into && echo true || echo false")?.toBoolean() ?: false
        if (exists) println("\n\uD83D\uDCE6 Directory [$into] is found on remote server. Will not be copied.")
        else println("\n\uD83D\uDCE6 Directory [$into] is not exist on remote server")
        return exists
    }

    private fun SessionHandler.remoteMkDir(into: String) = into.apply { execute("mkdir --parent $this") }
    private fun SessionHandler.remoteRm(vararg folders: String) = folders.forEach {
        println("> ✂️ Removing remote folder [$it]...")
        execute("rm -fr $it")
    }

    private fun SessionHandler.copyBack(file: String) {
        if (backend) backendServices.parallelStream()
            .forEach { copy(file, it) }
    }
    private fun SessionHandler.copyFront(file: String) = if (frontend) copy(file, frontendFolder) else false
    private fun SessionHandler.copyPostgres(file: String) = if (postgres) copy(file, postgresService) else false

    private fun ifNotGroovyThenKotlin(buildFile: String): String =
        if (File(buildFile).exists()) buildFile else "$buildFile.kts"

    private fun String.removeLocal() {
        File("${project.rootDir}/$this".normalizeForWindows()).apply {
            if (exists()) deleteRecursively()
            println("_ ✂️ Removed local folder [$this]")
        }
    }

    private fun deleteNodeModulesAndNuxtFolders() {
        val toRemoveLocal = setOf(".nuxt", ".idea", "node_modules", "package-lock.json", ".DS_Store")
        if (clearNuxt) toRemoveLocal.forEach { "$frontendFolder/$it".removeLocal() }
    }

    private fun SessionHandler.copy(file: String, remote: String = "") = copy(File(file), remote)

    private fun SessionHandler.copy(file: File, remote: String = ""): Boolean {
        val from = File("${project.rootDir}/$remote/$file".normalizeForWindows())
        val into = "${project.name}/$remote"
        if (from.exists()) {
            put(from, remoteMkDir(into))
            println("\uD83D\uDDA5️ FILE from local [$from] \n\t to remote {$into}")
            return true
        } else println("\t☣️ > Skip not found: $from\n")
        return false
    }

    private fun remote() = (server ?: if (host != null) SshServer(hostSsh = host!!, userSsh = user!!)
    else SshServer()).remote(checkKnownHosts)

    private fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))

    private fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
        session(*remotes, delegateClosureOf(action))
}


fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)

val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup)

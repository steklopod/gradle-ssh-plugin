package online.colaba

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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

open class Ssh : Cmd() {
    init {
        group = sshGroup
        description = "Publish by FTP your distribution with SSH commands"
    }

    @get:Input
    var jars: MutableSet<String> = mutableSetOf()

    @get:Input
    @Optional
    var host: String? = null

    @get:Input
    @Optional
    var user: String = "root"

    @get:Input
    @Optional
    var server: SshServer? = null

    @get:Input
    var frontendFolder: String = FRONTEND

    @get:Input
    var backendFolder: String = BACKEND

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
    var clearNuxt: Boolean = false

    @get:Input
    var postgres: Boolean = false

    @get:Input
    var monolit: Boolean = false

    @get:Input
    var admin: Boolean = false

    @get:Input
    var config: Boolean = false

    @get:Input
    var static: Boolean = false

    @get:Input
    var elastic: Boolean = false

    @get:Input
    var kibana: Boolean = false

    @get:Input
    var docker: Boolean = false

    @get:Input
    var gradle: Boolean = false

    @get:Input
    var nginx: Boolean = false

    @get:Input
    var withBuildSrc: Boolean = false

    @TaskAction
    fun run() {
        Ssh.newService().runSessions {
            session(remote()) {
                runBlocking {
                    if (clearNuxt) deleteNodeModulesAndNuxtFolders()
                    if (frontend) copyWithOverrideAsync(frontendFolder)

                    if (monolit) jars = mutableSetOf(backendFolder)
                    if (admin) jars.add(ADMIN_SERVER)
                    if (config) jars.add(CONFIG_SERVER)

                    if (postgres) {
                        copyIfNotRemote(POSTGRES)
                        if (!copyIfNotRemote("$POSTGRES/backups")) remoteMkDir("$POSTGRES/backups")
                        execute("chmod 777 -R ./${project.name}/$POSTGRES/backups")

                        copyPostgres("docker-entrypoint-initdb.d")
                        copyPostgres("postgresql.conf")
                    }
                    if (static) copyIfNotRemote(STATIC)

                    if (nginx) copyWithOverrideAsync(NGINX)

                    if (jars.isNotEmpty()) jars.parallelStream().forEach { copyWithOverride(jarLibFolder(it)) }

                    if (gradle) copyGradle()

                    if (docker) copyDockerInEach("docker-compose.yml", "Dockerfile", ".dockerignore", ".env")

                    if (elastic) {
                        listOf(
                            "elasticsearch.yml", ELASTIC_CERT_NAME,
                            "docker-compose.logstash.yml", "logstash.conf", "logstash.yml"
                        ).forEach { copy(it, ELASTIC) }
                        execute("chmod -R 777 ./${project.name}/$ELASTIC/$ELASTIC_CERT_NAME")

                        val elasticDataFolder = "$ELASTIC/$ELASTIC_DOCKER_DATA"
                        val elasticDockerVolumeFolder = "${project.name}/$elasticDataFolder"
                        if (!remoteExists(elasticDataFolder)) {
                            println("[$elasticDockerVolumeFolder] not exist. Creating with chmod 777")
                            remoteMkDir(elasticDockerVolumeFolder)
                            execute("chmod -R 777 ./$elasticDockerVolumeFolder")
                        }
                    }
                    if (kibana) listOf("kibana.yml", "docker-compose.kibana.yml").forEach { copy(it, ELASTIC) }

                    directory?.let { copyWithOverrideAsync(it) }

                    run?.let {
                        println("\n\uD83D\uDD11 Executing command on remote server: { $run }")
                        println(execute(it))
                    }
                }
            }
        }
    }

    private fun SessionHandler.copyDockerInEach(vararg files: String) = files.forEach { file ->
        copy(file); copyInFrontend(file); copyInAllBackends(file);
        if (postgres) copyPostgres(file)
        if (elastic) copy(file, ELASTIC)
    }

    private suspend fun SessionHandler.copyGradle() = coroutineScope {
        copyGradleWrapperIfNotExists()
        val buildFile = ifNotGroovyThenKotlin("build.gradle")
        val settingsFile = ifNotGroovyThenKotlin("settings.gradle")
        copy(settingsFile)
        copy(buildFile)
        copy("gradle.properties")

        copyInFrontend(buildFile)

        copyInAllBackends(buildFile)
        copyInAllBackends(settingsFile)

        if (withBuildSrc) {
            val buildSrc = "buildSrc"
            "$buildSrc/build".removeLocal()
            copyWithOverrideAsync(buildSrc)
        }
    }

    private suspend fun SessionHandler.copyGradleWrapperIfNotExists() {
        if (copyIfNotRemote("gradle")) {
            copy("gradlew")
            copy("gradlew.bat")
            execute("chmod +x ${project.name}/gradlew")
        }
    }

    private suspend fun SessionHandler.copyIfNotRemote(directory: String = ""): Boolean =
        remoteExists(directory).apply { if (!this) copyWithOverrideAsync(directory) }


    private suspend fun SessionHandler.copyWithOverrideAsync(directory: String = "") = coroutineScope {
        async { copyWithOverride(directory) }
    }

    private fun SessionHandler.copyWithOverride(directory: String): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        val localFileExists = File(fromLocalPath).exists()
        if (localFileExists) {
            println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] \n\t  to remote {$toRemote}")
            remoteRm(toRemote)
            val toRemoteParent = File(toRemote).parent.normalizeForWindows()
            println("> \uD83D\uDDC3️ Copy [${fromLocalPath.substringAfterLast('/')}] into remote {$toRemoteParent} in progress...\n")
            put(File(fromLocalPath), remoteMkDir(toRemoteParent))
        } else println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] not exists, so it not will be copied to server.")
        return localFileExists
    }

    private fun SessionHandler.put(from: Any, into: String) = put(hashMapOf("from" to from, "into" to into))

    private fun SessionHandler.remoteExists(remoteFolder: String): Boolean {
        val exists = execute("test -d ${project.name}/$remoteFolder && echo true || echo false")?.toBoolean() ?: false
        if (exists) println("\n\uD83D\uDCE6 Directory [$remoteFolder] is EXISTS on remote server.")
        else println("\n \uD83D\uDCE6 Directory [$remoteFolder] is NOT EXISTS on remote server.")
        return exists
    }

    private fun SessionHandler.remoteMkDir(directory: String) = execute("mkdir --parent $directory")
    private fun SessionHandler.remoteRm(vararg folders: String) = folders.forEach {
        println("> ✂️ Removing remote folder [$it]...")
        execute("rm -fr $it")
    }

    private fun SessionHandler.copyInAllBackends(file: String) {
        jars.forEach { copy(file, it) }
    }

    private fun SessionHandler.copyInFrontend(file: String) = if (frontend) copy(file, frontendFolder) else false
    private fun SessionHandler.copyPostgres(file: String) = if (postgres) copy(file, POSTGRES) else false

    private fun ifNotGroovyThenKotlin(buildFile: String): String =
        if (File(buildFile).exists()) buildFile else "$buildFile.kts"

    private fun String.removeLocal() {
        File("${project.rootDir}/$this".normalizeForWindows()).apply {
            if (exists()) deleteRecursively()
            println("_ ✂️ Removed local folder [$this]")
        }
    }

    private fun deleteNodeModulesAndNuxtFolders() = setOf(".nuxt", ".idea", "node_modules", ".DS_Store")
        .forEach { "$frontendFolder/$it".removeLocal() }


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

    private fun remote(): Remote {
        val sshServer = if (host != null) SshServer(hostSsh = host!!, userSsh = user)
        else SshServer()
        return (server ?: sshServer).remote(checkKnownHosts)
    }

    private fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))

    private fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
        session(*remotes, delegateClosureOf(action))
}


fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)

val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup)

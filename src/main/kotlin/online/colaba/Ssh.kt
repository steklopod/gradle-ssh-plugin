package online.colaba

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import org.slf4j.LoggerFactory
import java.io.File

const val sshGroup = "ssh"

open class Ssh : Cmd() {
    init {
        group = sshGroup
        description = "Publish by FTP your distribution with SSH commands"
    }
    @get:Input var host: String = DEFAULT_HOST
    @get:Input var user: String = "root"

    @get:Input var run : String = "cd ${project.name} && echo \$PWD"

    @get:Input var frontendFolder      : String  = FRONTEND
    @get:Input var backendFolder       : String  = BACKEND
    @get:Input var jars                : MutableSet<String> = mutableSetOf()
    @get:Input @Optional var directory : String? = null

    @get:Input var monolit          : Boolean = false
    @get:Input var gradle           : Boolean = false
    @get:Input var docker           : Boolean = false
    @get:Input var frontend         : Boolean = false
    @get:Input var clearNuxt        : Boolean = false
    @get:Input var postgres         : Boolean = false
    @get:Input var nginx            : Boolean = false
    @get:Input var static           : Boolean = false
    @get:Input var elastic          : Boolean = false
    @get:Input var kibana           : Boolean = false
    @get:Input var admin            : Boolean = false
    @get:Input var config           : Boolean = false
    @get:Input var withBuildSrc     : Boolean = false
    @get:Input var checkKnownHosts  : Boolean = false
    @get:Input @Optional var server : SshServer? = null

    @TaskAction fun run() { Ssh.newService().runSessions { session(remote()) { runBlocking {
    println("Remote folder: 🧿${project.name}🧿")
    println("HOST: $host ")
    println("USER: $user ")

    if (static) copyIfNotRemote(STATIC)
    if (nginx)  copyWithOverrideAsync(NGINX)

    if (clearNuxt) deleteNodeModulesAndNuxtFolders()
    if (frontend)  copyWithOverrideAsync(frontendFolder)

    if (postgres) {
        copyIfNotRemote(POSTGRES)
        if (!copyIfNotRemote("$POSTGRES/backups")) remoteMkDir("${project.name}/$POSTGRES/backups")
        execute("chmod 777 -R ./${project.name}/$POSTGRES/backups")
        copyPostgres("docker-entrypoint-initdb.d")
        copyPostgres("postgresql.conf")
    }
    // Jars
    if (admin)   jars.add(ADMIN_SERVER)
    if (config)  jars.add(CONFIG_SERVER)
    if (monolit) jars = mutableSetOf(backendFolder)
    if (jars.isNotEmpty()) jars.parallelStream().forEach { copyWithOverride(jarLibFolder(it)) }

    if (gradle) copyGradle()

    if (docker) copyInEach("docker-compose.yml", "Dockerfile", ".dockerignore", ".env")

    if (elastic) {
        listOf("elasticsearch.yml", ELASTIC_CERT_NAME,
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

    println("🔮 Executing command on remote server [ $host ] 🔮 \n🔮 {{ `$run` }} 🔮\n" + execute(run) +"\n\n")

} } } }


    private fun SessionHandler.copyInBackends    (file: String) { jars.forEach { copy(file, it) } }
    private fun SessionHandler.copyInFrontend    (file: String)  = if (frontend) copy(file, frontendFolder) else false
    private fun SessionHandler.copyPostgres      (file: String)  = if (postgres) copy(file, POSTGRES) else false
    private fun SessionHandler.copyInEach(vararg files: String) = files.forEach { file ->
        copy(file); copyInFrontend(file); copyInBackends(file);
        if (postgres) copyPostgres(file); if (elastic) copy(file, ELASTIC); if (postgres) copy(file, POSTGRES);
        if (nginx) copy(file, NGINX); if (admin) copy(file, ADMIN_SERVER); if (config) copy(file, CONFIG_SERVER)
    }

    private suspend fun SessionHandler.copyGradle() = coroutineScope {
        fun ifNotGroovyThenKotlin(buildFile: String): String = (if (File(buildFile).exists()) buildFile else "$buildFile.kts").apply{
                copyInEach(this) }
        copy("gradle")
        copy("gradlew")
        copy("gradlew.bat")
        copy("gradle.properties")
        ifNotGroovyThenKotlin("build.gradle")
        ifNotGroovyThenKotlin("settings.gradle")
        execute("chmod +x ${project.name}/gradlew")
        if (withBuildSrc) "buildSrc".run { "$this/build".removeLocal(); copyWithOverrideAsync(this) }
    }


    private suspend fun SessionHandler.copyIfNotRemote(directory: String = ""): Boolean =
        remoteExists(directory).apply { if (!this) copyWithOverrideAsync(directory) }

    private fun SessionHandler.copyWithOverride(directory: String = ""): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        val localFileExists = File(fromLocalPath).exists()
        if (localFileExists) {
            println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] \n\t  to remote {$toRemote}")
            removeRemote(toRemote)
            val toRemoteParent = File(toRemote).parent.normalizeForWindows()
            println("> \uD83D\uDDC3️ Copy [${fromLocalPath.substringAfterLast('/')}] into remote {$toRemoteParent} in progress...\n")
            put(File(fromLocalPath), remoteMkDir(toRemoteParent))
        } else println("\n\uD83D\uDCE6 FOLDER local [$fromLocalPath] not exists, so it not will be copied to server.")
        return localFileExists
    }
    private suspend fun SessionHandler.copyWithOverrideAsync(directory: String) =
        coroutineScope { launch { copyWithOverride(directory) } }

    private fun SessionHandler.put(from: Any, into: String) = put(hashMapOf("from" to from, "into" to into))

    private fun SessionHandler.remoteExists(remoteFolder: String): Boolean {
        val exists = execute("test -d ${project.name}/$remoteFolder && echo true || echo false")?.toBoolean() ?: false
        if (exists) println("\n\uD83D\uDCE6 Directory [$remoteFolder] is EXISTS on remote server.")
        else println("\n \uD83D\uDCE6 Directory [$remoteFolder] is NOT EXISTS on remote server.")
        return exists
    }

    private fun SessionHandler.remoteMkDir(into: String) = into.apply { execute("mkdir --parent $this") }
    private fun SessionHandler.removeRemote(vararg folders: String) = folders.forEach {
        execute("rm -fr $it"); println("🗑️️ Removed REMOTE folder [ $it ] 🗑️️")
    }
    private fun String.removeLocal() { File("${project.rootDir}/$this".normalizeForWindows()).apply {
        if (exists()) deleteRecursively(); println("✂️ Removed LOCAL folder: [ $this ] ✂️")
    } }

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
    private fun SessionHandler.copy(file: String, remote: String = "") = copy(File(file), remote)

    private fun deleteNodeModulesAndNuxtFolders() = setOf(".nuxt", ".idea", "node_modules", ".DS_Store").forEach { "$frontendFolder/$it".removeLocal() }

    private fun remote(): Remote = (server ?: SshServer(hostSsh = host, userSsh = user)).remote(checkKnownHosts)
    private fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))
    private fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) = session(*remotes, delegateClosureOf(action))
}


fun Project.registerSshBackendTask() = tasks.register<online.colaba.Ssh>("sshBackend")
val Project.sshBackend: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>("sshBackend"){
        description = "Copy [$BACKEND] jar to remote server"
        monolit = true
    }

fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)
val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup){
        description = "Template for SSH deploy. All props are set to `false`"
    }

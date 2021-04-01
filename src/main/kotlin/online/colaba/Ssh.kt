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

    @get:Input @Optional var postgres  : String? = null
    @get:Input @Optional var directory : String? = null

    @get:Input var jars             : Set<String> = setOf()
    @get:Input var gradle           : Boolean = false
    @get:Input var docker           : Boolean = false
    @get:Input var frontend         : Boolean = false
    @get:Input var clearNuxt        : Boolean = false
    @get:Input var nginx            : Boolean = false
    @get:Input var static           : Boolean = false
    @get:Input var elastic          : Boolean = false
    @get:Input var kibana           : Boolean = false
    @get:Input var admin            : Boolean = false
    @get:Input var config           : Boolean = false
    @get:Input var withBuildSrc     : Boolean = false
    @get:Input var checkKnownHosts  : Boolean = false
    @get:Input @Optional var server : SshServer? = null

    @TaskAction fun run() {
    println("🔜 Remote folder: 🧿${project.name}🧿")
    println("HOST: $host ")
    println("USER: $user ")
    Ssh.newService().runSessions { session(remote()) { runBlocking {

    val isInitRun = remoteExists("")
    var frontendFolder: String? = null

    if (isInitRun) println("🎉 🎉 🎉 INIT RUN 🎉 🎉 🎉") else println("🍄🍄🍄 REDEPLOY STARTED...")

    fun copyInEach(vararg files: String) = files.forEach { file ->
        copy(file)
        jars.forEach { copy(file, it) }
        postgres?.run { copy(file, this) }
        frontendFolder?.run {copy(file, this); copy(file, NGINX) }
        if (elastic) copy(file, ELASTIC)
    }

    suspend fun copyGradle() = coroutineScope {
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

    if (static) !copyIfNotRemote(STATIC)
    if (nginx) copyWithOverrideAsync(NGINX)

    frontendName()?.run {
        if (frontend) {
            println("📣 Found local frontend [$this] ⬅️ folder 📣\n")
            frontendFolder = this
            if (clearNuxt) {
                println("Removing local files from [$frontendFolder] ⬅️:")
                deleteNodeModulesAndNuxtFolders(this)
            }
            copyWithOverrideAsync(this)
        }
    }

    postgresName()?.run {
        postgres = this
        println("🌀 POSTGRES folder: [$this] 🌀")
        if (remoteExists(this)) {
            copy("docker-entrypoint-initdb.d", this)
            copy("postgresql.conf", this)
        } else copyWithOverride(this)

        val backupsFolder = "$this/backups"
        if (!remoteExists(backupsFolder)) {
            if (project.localExists(backupsFolder)) {
                execute("chmod 777 -R ./$backupsFolder")
                copyWithOverride(backupsFolder)
            } else remoteMkDir("${project.name}/$backupsFolder")
            println("\n🔆 BACKUPS folder [$backupsFolder] now is on remote server 🔆 \n")
        }
    }

    if (jars.isEmpty()) jars = project.subprojects.filter { !it.name.endsWith("lib") && it.localExists("src/main")  }.map { it.name }.toSet()
    if (jars.isEmpty()) System.err.println("⚰️ Can't find java/kotlin backends in subprojects.")
    else println("\n🍐🥝️🍌 Current BACKENDS: $jars \n")
    jars.parallelStream().forEach { copyWithOverride(jarLibFolder(it)) }

    if (gradle) copyGradle()

    if (docker) copyInEach("docker-compose.yml", "Dockerfile", ".dockerignore"/*, ".env"*/)

    if (elastic) {
        listOf("elasticsearch.yml", ELASTIC_CERT_NAME,
               "docker-compose.logstash.yml", "logstash.conf", "logstash.yml"
        ).forEach { copy(it, ELASTIC) }
        execute("chmod -R 777 ./${project.name}/$ELASTIC/$ELASTIC_CERT_NAME")

        val elasticDataFolder = "$ELASTIC/$ELASTIC_DOCKER_DATA"
        val elasticDockerVolumeFolder = "${project.name}/$elasticDataFolder"
        if (!remoteExists(elasticDataFolder)) {
            println("🤖 [$elasticDockerVolumeFolder] not exist")
            remoteMkDir(elasticDockerVolumeFolder)
        }
    }
    if (kibana) listOf("kibana.yml", "docker-compose.kibana.yml").forEach { copy(it, ELASTIC) }

    directory?.let { copyWithOverrideAsync(it) }

    println("\n🔮 Executing command on remote server [ $host ]🔜🔜🔜 ")
    println("🔮🔮🔮$run🔮🔮🔮")
    println("🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮")
    println("🔮🔮🔮 RESULT: " + execute(run) +"🔮🔮🔮")
    println("🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮🔮")
    println("🩸🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🩸🩸🩸")
    println("🩸🩸🔫🔫🔫🔫 N I C E 🔫🔫🔫🔫🩸🩸")
    println("🩸🩸🩸🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🩸")


} } } }

    fun findInSubprojects(file: String) = project.subprojects.firstOrNull { it.localExists(file) }?.name
    fun postgresName(): String? = postgres
                ?: findInSubprojects("postgresql.conf") ?: findInSubprojects("docker-entrypoint-initdb.d")
                ?: project.subprojects.map { it.name }.firstOrNull { it.startsWith("postgres")}

    fun frontendName() = findInSubprojects ( "package.json") ?: project.subprojects.map { it.name }
        .firstOrNull { it.startsWith("front") }


     suspend fun SessionHandler.copyIfNotRemote(directory: String = ""): Boolean =
        remoteExists(directory).apply { if (!this) copyWithOverrideAsync(directory) }

     fun SessionHandler.copyWithOverride(directory: String = ""): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        val localFileExists = File(fromLocalPath).exists()
        if (localFileExists) {
            removeRemote(toRemote)
            val toRemoteParent = File(toRemote).parent.normalizeForWindows()
            put(File(fromLocalPath), remoteMkDir(toRemoteParent))
            println("🗃️ Deploy local folder [$directory] ⬅️\n\t into remote 🔜 {$toRemoteParent}/[$directory]    is done\n")
        } else println("📦 LOCAL folder [$directory] ⬅️ NOT EXISTS, so it not will be copied to server.")
        return localFileExists
    }

     suspend fun SessionHandler.copyWithOverrideAsync(directory: String) =
        coroutineScope { launch { copyWithOverride(directory) } }

     fun SessionHandler.put(from: Any, into: String) = put(hashMapOf("from" to from, "into" to into))

     fun SessionHandler.remoteExists(remoteFolder: String): Boolean {
        val exists = execute("test -d ${project.name}/$remoteFolder && echo true || echo false")?.toBoolean() ?: false
        if (exists) println("\n🧱 Directory [$remoteFolder]🔜 is EXISTS on remote server.")
        else println("\n📦 Directory [$remoteFolder]🔜 is NOT EXISTS on remote server.")
        return exists
    }

     fun SessionHandler.remoteMkDir(into: String) = into.normalizeForWindows().apply { if(!into.contains(".")) execute("mkdir --parent $this") else println("`.` dot int path: [$into] - will not run command: [mkdir --parent $this]") }
     fun SessionHandler.removeRemote(vararg folders: String) = folders.forEach {
        execute("rm -fr $it"); println("🗑️️ Removed REMOTE folder 🔜 [ $it ] 🗑️️")
    }
     fun String.removeLocal() { File("${project.rootDir}/$this".normalizeForWindows()).apply {
        if (exists()) deleteRecursively(); println("✂️ Removed LOCAL folder: [ $this ] ✂️")
    } }

     fun SessionHandler.copy(file: File, remote: String = ""): Boolean {
        val from = File("${project.rootDir}/$remote/$file".normalizeForWindows())
        val into = "${project.name}/$remote"
        if (from.exists()) {
            put(from, remoteMkDir(into))
            println("💾️ FILE from local [ $remote/${file.name} ] ⬅️ \n\t to remote 🔜 {$into}")
            return true
        } else println("\t 🪠 Skip not found (local) ⬅️: $remote/${file.name} ")
         return false
     }
     fun SessionHandler.copy(file: String, remote: String = "") = copy(File(file), remote)

     fun deleteNodeModulesAndNuxtFolders(frontendLocalFolder: String) = setOf(".nuxt", ".idea", "node_modules", ".DS_Store").forEach { "$frontendLocalFolder/$it".removeLocal() }

     fun remote(): Remote = (server ?: SshServer(hostSsh = host, userSsh = user)).remote(checkKnownHosts)
     fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))
     fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) = session(*remotes, delegateClosureOf(action))
}

fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)
val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup){
        description = "Template for SSH deploy. All props are set to `false`"
    }

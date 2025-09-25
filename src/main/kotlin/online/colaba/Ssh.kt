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
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.system.measureTimeMillis

const val sshGroup = "ssh"

open class Ssh : Cmd() {
    init {
        group = sshGroup
        description = "🐸 Deploy by FTP your distribution with SSH commands"
    }
    @get:Input var user                : String = "root"
    @get:Input @Optional var host      : String? = null

    @get:Input @Optional var postgres  : String? = null
    @get:Input @Optional var directory : String? = null

    @get:Input var run : String = "cd ${project.name} && echo \$PWD"

    @get:Input var docker : Boolean = false

    @get:Input var gradle  : Boolean = false
    @get:Input var backend : Boolean = false
    @get:Input var newrelic: Boolean = false
    @get:Input var jars    : List<String> = listOf()

    @get:Input var allProjects                : Boolean = false

    @get:Input var frontend                   : Boolean = false
    @get:Input var frontendWhole              : Boolean = false
    @get:Input var frontendClear              : Boolean = false
    @get:Input var frontendClearLock          : Boolean = false
    @get:Input var frontendClearOnly          : Boolean = false
    @get:Input var frontendDistCompressedType : String = ".tar.xz"
    @get:Input var frontendDist               : String = ".output"
    @get:Input @Optional var frontendFolder   : String? = null

    @get:Input var nginx           : Boolean = false
    @get:Input var monitoring      : Boolean = false
    @get:Input var static          : Boolean = false
    @get:Input var staticOverride  : Boolean = false
    @get:Input var elastic         : Boolean = false
    @get:Input var kibana          : Boolean = false
    @get:Input var admin           : Boolean = false
    @get:Input var envFiles        : Boolean = false
    @get:Input var broker          : Boolean = false
    @get:Input var config          : Boolean = false
    @get:Input var withBuildSrc    : Boolean = false
    @get:Input var checkKnownHosts : Boolean = false
    @get:Input @Optional var server : SshServer? = null

@TaskAction fun run() {
    if (frontendClearOnly) { frontendName()?.run {
            println("[frontend] ✂️: removing local frontend temporary files from [$this] ⬅️:")
            clearFrontendTempFiles(this)
            clearFrontendLockFile(this)
            "$this/$frontendDist".removeLocal()
        }
        return
    }

    println("🔜 REMOTE FOLDER: 🧿${project.name}🧿")
    host = host ?: project.computeHostFromGroup()
    println("HOST: $host , USER: $user")
    validateHost(host!!)

    try {
        ProcessBuilder().apply { directory(project.rootDir.absoluteFile); command(cmdPrefix + listOf("chmod", "400", "id_rsa")) }.start()
    } catch (e: Exception) {
        System.err.println("⛔️ Error running `chmod 400 id_rsa` in folder ${project.rootDir}: ${e.message}")
    }

    Ssh.newService().runSessions { session(remote()) { runBlocking {

    val isInitRun = !remoteExists("")
    if (isInitRun) println("\n🎉 🎉 🎉 INIT RUN 🎉 🎉 🎉\n") else println("\n🍄🍄🍄 REDEPLOY STARTED 🍄🍄🍄\n")

    fun copyInEach(vararg files: String) = measureTimeMillis { files.forEach { file ->
        copy(file)
        if (jars.isEmpty() || allProjects) findJARs(); jars.stream().parallel().forEach { copy(file, it) }
        if (frontend || allProjects) frontendName()?.run { copy(file, this) }
        if (nginx || allProjects) copy(file, NGINX)
        if (elastic || allProjects) copy(file, ELASTIC)
        postgres ?: postgresName()?.run { copy(file, this) }
       /* TODO:
            - monitoring,
       */
    }}.apply {
        statistic["IN EACH project"] = this
        println("\n\t  ⏱️ ${MILLISECONDS.toSeconds(this)} sec. (or $this ms) - copy IN EACH project \n") }

    suspend fun copyGradle() = coroutineScope { measureTimeMillis {
        fun ifNotGroovyThenKotlin(buildFile: String): String = (if (File(buildFile).exists()) buildFile else "$buildFile.kts")
                .apply{ copyInEach(this) }
            copy("gradle")
            copy("gradlew")
            copy("gradlew.bat")
            copy("gradle.properties")
            ifNotGroovyThenKotlin("build.gradle")
            ifNotGroovyThenKotlin("settings.gradle")
            execute("chmod +x ${project.name}/gradlew")
            if (withBuildSrc) "buildSrc".run { "$this/build".removeLocal(); copyWithOverrideAsync(this) }
        }.apply {
            statistic["GRADLE"] = this
            println("⏱️ ${MILLISECONDS.toSeconds(this)} sec. (or $this ms) - \uD83D\uDCBF GRADLE") }
    }

    /////////////////////////
    //////// START  ////////
    ///////////////////////
    if (staticOverride) copyWithOverrideAsync(STATIC)
    if (static) !copyIfNotRemote(STATIC)

    if (monitoring) copyAllFilesFromFolder("monitoring")

    if (broker) copyAllFilesFromFolder(BROKER)

    if (nginx) copyAllFilesFromFolder(NGINX)

    if (frontend) { frontendName()?.run {
        println("\n📣 Found local frontend folder in subprojects: [$this] ⬅️  📣\n")

        val archiveFolderInRoot = "$frontendDist$frontendDistCompressedType"
        val archiveFolder = "$this/$archiveFolderInRoot"

        if (frontendClearLock) clearFrontendLockFile(this)
        if (frontendClear) clearFrontendTempFiles(this)

        val distributionDirectory: String = if (frontendWhole) {
            println("🦖🦖🦖 Frontend whole root folder will be deployed: [ $this ]")
            copyWithOverride(this)
            this
        } else {
            println("🦖 FRONTEND: only distribution of $this will be copied...")
            if (project.localExists(archiveFolder)) {
                println("\n\n👍🏻 Compressed FRONTEND distribution found: \n 📺🚀[ $archiveFolder ]\n")
                copyWithOverride(archiveFolder)
                // removeRemote("${project.name}/$this/$frontendDist")
                // execute("tar -xf ${project.name}/$archiveFolder --directory ./${project.name}/$this")
                archiveFolder
            } else if (project.localExists(archiveFolderInRoot)) {
                println("\n\n🗜🦖 Compressed WHOLE frontend distribution found: [ $archiveFolder ]")
                copyWithOverride(archiveFolderInRoot)
                execute("tar -xf ${project.name}/$archiveFolderInRoot --directory ./${project.name}")
                archiveFolderInRoot
            } else {
                val frontendOutput = "$this/$frontendDist"
                System.err.println("\n\n🗜🦭 Frontend zip-archive folder NOT found [ $archiveFolder ]. Searching in other places like [$frontendOutput]...")

                if (project.localExists(frontendOutput)) {
                    println("🗜🦭🔧 Frontend distribution folder found [ $frontendOutput ].")
                    copyWithOverride(frontendOutput)
                    frontendOutput
                } else {
                    System.err.println("\n\n 🦖🚸🦖 Frontend whole root folder will be deployed: [ $this ] because no distribution is found")
                    copyWithOverride(this)
                    this
                }
            }
        }
        println("🌈 FRONTEND DISTRIBUTION : [ $distributionDirectory ]")

        println("🐳 Start copying Docker files for frontend:")
        listOf("docker-compose.yml", "docker-compose.yaml", "compose.yml", "compose.yaml", "Dockerfile", ".dockerignore")
            .filter { project.localExists("$this/$it") }
            .forEach {
                println("🐳 Docker file in frontend will be copied: $it")
                copy(it, this)
            }
    } }

    postgres?.run {
        val folder = if (project.localExists(this)) this else postgresName()
        if (folder == null) {
            println(">>> local [postgres] folder not found"); return@run
        }
        postgres = folder
        println("\n🌀 Found local POSTGRES folder: [$folder] 🌀")
        if (remoteExists(folder)) {
            copy(postgresConfigFolder, folder)
        } else copyWithOverride(folder)

        val folderBackups = "$folder/backups"
        if (!remoteExists(folderBackups)) {
            if (project.localExists(folderBackups)) copyWithOverride(folderBackups)
            else remoteMkDir("${project.name}/$folderBackups")
            execute("chmod -R 777 ./${project.name}/$folderBackups")
            println("\n🔆 BACKUPS folder [$folderBackups] now is on remote server 🔆 \n")
    } }

    if (backend) measureTimeMillis {
        findJARs()
        println("\uD83C\uDF4C Start deploying JARs...")
        jars.parallelStream().forEach {
            copyWithOverride(jarLibFolder(it))
            if (newrelic) copyWithOverride("$it/newrelic")
        }

    }.apply {
        statistic["JARS ($jars)"] = this; println("⏱️ ${MILLISECONDS.toSeconds(this)} sec. (or $this ms) - \uD83C\uDF4C JARS \n") }


     if (gradle) launch { copyGradle() }

     if (docker) launch {
         listOf("docker-compose.infra.yml", "compose.infra.yml").any { copy(it) }
         listOf("docker-compose.prod.yml", "compose.prod.yml").any { copy(it) }
         listOf("elastic/docker-compose.yml", "elastic/compose.yml").any { copy(it) }
         listOf("nginx/docker-compose.yml", "nginx/compose.yml").any { copy(it) }
         copyInEach("docker-compose.yml", "docker-compose.yaml", "compose.yml", "compose.yaml", "Dockerfile", ".dockerignore")
     }

    if (elastic && project.localExists(ELASTIC)) { measureTimeMillis {
        println("\n💿 Start [$ELASTIC]... ")
        if (!docker) {
            listOf("docker-compose.infra.yml", "compose.infra.yml").any { copy(it) }
            listOf("elastic/docker-compose.yml", "elastic/compose.yml").any { copy(it) }
        }
        copy("elasticsearch.yml", ELASTIC)
        copy("filebeat", ELASTIC)
        copy("kibana", ELASTIC)
        copy("logstash", ELASTIC)
        /*
            if (logstash) { copy("logstash", ELASTIC); copy("filebeat", ELASTIC) }
            if (kibana) copy("kibana", ELASTIC)
            copy("agent", ELASTIC)
        */

        // certificates
        val certFolder = "$ELASTIC/$ELASTIC_CERTS_FOLDER"
        if (project.localExists(certFolder)) {
            println("🔩 Start copying elastic certificates whole folder")
            copy(ELASTIC_CERTS_FOLDER, ELASTIC)
            execute("chmod +x ./${project.name}/$certFolder/ca.crt")
            execute("chmod +x ./${project.name}/$certFolder/ca.key")
        } else println("🫵🏼There is no [ $certFolder ] folder! If you want to copy elastic certs - put `ca.crt` + `ca.key` certs in this folder")
        // elastic-data folder: create and chmod
        val volumeFolder = "$ELASTIC/$ELASTIC_DOCKER_VOLUME"
        val volumeFolderFull = "${project.name}/$volumeFolder"
        if (!remoteExists(volumeFolder)) {
            println("🤖 [$ELASTIC_DOCKER_VOLUME] not exist in [$ELASTIC]. 🤖🤖🤖 I've just created empty folder.")
            remoteMkDir(volumeFolderFull)
            execute("chmod 777 -R ./$volumeFolderFull")
        }
        println("💿 OK: Copying everything inside [$ELASTIC] is done\n")
    }.apply {
        statistic["ELASTIC"] = this
        println("⏱️ ${MILLISECONDS.toSeconds(this)} sec. (or $this ms) - \uD83D\uDCBF ELASTIC") }
    }

    if (envFiles) launch { copyInEach( ".env") }

    directory?.let { copyWithOverrideAsync(it) }


    if (newrelic) measureTimeMillis {
        findJARs()
        println("🚀 Start deploying New Relic only...")
        jars.forEach { copyWithOverride("$it/newrelic") }
    }.apply {
        statistic["NEW RELIC"] = this
        println("⏱️ ${MILLISECONDS.toSeconds(this)} sec. - 🚀 NEW RELIC\n")
    }

//////////////////////////////////// END
    }
    println("\n🔮 Executing command on remote server [ $host ]:")
    println("\t🔜🔜🔜 $run")
    println("\n🔮🔮🔮🔮🔮🔮🔮")
    println("🔮🔮🔮 RESULT: " + execute(run))
    println("🔮🔮🔮🔮🔮🔮🔮")
    } }

    printDurationStatistic()

    println("\n🩸🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🩸🩸🩸")
    println("🩸🩸🔫🔫🔫 C O L A B A 🔫🔫🔫🩸🩸")
    println("🩸🩸🩸🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🩸\n")
}

    private suspend fun SessionHandler.copyAllFilesFromFolder(fromFolder: String) = coroutineScope {
        if (remoteExists(fromFolder)) {
            println("🔘🔃 Copying [${fromFolder.uppercase(getDefault())}] nested files...")
            val folder = File("${project.rootDir.absolutePath}/$fromFolder")
            folder.walk()
                .filter { !it.isDirectory && it.name != ".gitignore" && it.extension != "md" }
                .map { it.path.substringAfter("$folder/") }
                .filter { !it.endsWith(".md") }
                .forEach { copy(it, fromFolder) }
        } else {
            println("⚡⚡⚡ Copying WHOLE [$fromFolder] folder...")
            copyWithOverride(fromFolder)
        }
    }

    private fun findInSubprojects(file: String) = project.subprojects
        .sortedByDescending { it.name.contains("front") }
        .firstOrNull { it.localExists(file) }?.name

    private fun findJARs() {
        if (jars.isEmpty()) jars =
            project.subprojects.filter { it.localExists("src/main") && !it.name.endsWith("lib") }.map { it.name }
        if (jars.isEmpty()) System.err.println("⚰️⚰️⚰️ Can't find java/kotlin backend in subprojects !")
        else println("\n🍐🥝️🍌 Current BACKENDS: $jars \n")
    }

    private fun frontendName(): String? {
        if (frontendFolder != null) return frontendFolder
        var frontendFolder = findInSubprojects("package.json")
            ?: project.subprojects.map { it.name }.firstOrNull { it.startsWith("front") || it.endsWith("frontend") }
        if (frontendFolder == null) {
            val frontFoundFolder: String? = arrayOf("frontend", "front").find { project.localExists(it) }
            if (frontFoundFolder == null) System.err.println("Frontend folder not found in current project")
            else frontendFolder = frontFoundFolder
        }
        return frontendFolder
    }


    private fun postgresName(): String? = findInSubprojects(postgresConfigFile) ?: findInSubprojects("docker-entrypoint-initdb.d")
        ?: project.subprojects.map { it.name }.firstOrNull { it.startsWith(postgresConfigFolder) }

     private suspend fun SessionHandler.copyIfNotRemote(directory: String = ""): Boolean =
        remoteExists(directory).apply { if (!this) copyWithOverrideAsync(directory) }

     private fun SessionHandler.copyWithOverride(directory: String = ""): Boolean {
        val toRemote = "${project.name}/$directory"
        val fromLocalPath = "${project.rootDir}/$directory".normalizeForWindows()
        val localFileExists = File("${project.rootDir.absolutePath}/$directory").exists()
        if (localFileExists) {
            removeRemote(toRemote)
            val toRemoteParent = File(toRemote).parent.normalizeForWindows()
            val into = remoteMkDir(toRemoteParent)
            println("\n🚚 Deploy of [$directory] 🚠 just has STARTED. Wait a little ⏱️⏱️⏱️...\n")
            put(File(fromLocalPath), into)
            println("🚚✅️ Deploy of [$directory] ⬅️ into remote  {$toRemoteParent} is done\n")
        } else println("\n📦📌 LOCAL folder ☝️[$directory] ⬅️ NOT EXISTS, so it not will be copied to remote server.\n")
        return localFileExists
    }

     private suspend fun SessionHandler.copyWithOverrideAsync(directory: String) =
        coroutineScope { launch { copyWithOverride(directory) } }

    private fun SessionHandler.put(from: Any, into: String) = measureTimeMillis {
         put(hashMapOf("from" to from, "into" to into))
     }.run {
        val key = from.toString().substringAfter(project.name)
        println("⏱️ ${MILLISECONDS.toSeconds(this)} sec. (or $this ms) copy [ $key ]")
        statistic[key] = this
     }

    private fun SessionHandler.remoteExists(remoteFolder: String): Boolean {
        val exists = execute("test -d ${project.name}/$remoteFolder && echo true || echo false")?.toBoolean() ?: false
        if (exists) println("\n\uD83C\uDF1A Directory [${project.name}/$remoteFolder]🔜 EXISTS on remote server")
        else println("\n📦 Directory [${project.name}/$remoteFolder]🔜 does NOT EXIST on remote server")
        return exists
    }

     private fun SessionHandler.remoteMkDir(into: String) = into.normalizeForWindows().apply { execute("mkdir --parent $this") }
     private fun SessionHandler.removeRemote(vararg folders: String) = folders.forEach {
        execute("rm -fr $it"); println("🗑️️ Removed REMOTE folder 🔜 [ $it ] 🗑️️")
    }
    private fun String.removeLocal() {
        val file = File("${project.rootDir}/$this".normalizeForWindows())
        if (file.exists()) {
            file.deleteRecursively()
            println("✂️ Removed LOCAL folder: [ $this ] ✂️")
        } else println("\t...nothing to remove ✂️ locally ⬅️ for: [$this]")
    }

     private fun SessionHandler.copy(file: File, remote: String = ""): Boolean {
         val from = File("${project.rootDir}/$remote/$file".normalizeForWindows())
         val into = "${project.name}/$remote".normalizeForWindows()
         val name = file.name
         if (from.exists()) {
            put(from, remoteMkDir(into))
            println("💾️ FILE from local ├ $from →️ \n\t →️ to remote: {$into}╏$name╏")
            return true
        } else println("\t\t\t\t\t ... ✓ Skip not found file: ❏ $remote/$name ")
        return false
     }

    private fun SessionHandler.copy(file: String, remote: String = ""): Boolean {
        var from = file
        val targetFolder = if (file.count { it == '/' } > 0 && !remote.contains("/")) {
            from = file.substringAfterLast("/")
            "$remote/$file".substringBeforeLast(from)
        } else remote
        return copy(File(from), targetFolder)
    }

    private fun clearFrontendTempFiles(frontendLocalFolder: String) {
        println("❗️Frontend: Removing LOCAL temporary/autogenerated files from: [ $this ] folder. They are unnecessary for SCP deploy by ssh ✂️...")
        setOf(".nuxt", "node_modules", "dist")
            .forEach { "$frontendLocalFolder/$it".removeLocal() }
        if (frontendWhole) "$this/$frontendDist".removeLocal()
    }

    private fun clearFrontendLockFile(frontendLocalFolder: String) {
        val lockFile = "$frontendLocalFolder/package-lock.json"
        println("❗Frontend: Removing LOCAL $lockFile file ✂️...")
        lockFile.removeLocal()
        val pnpmLockFile = "$frontendLocalFolder/pnpm-lock.yaml"
        println("❗Frontend: Removing LOCAL $pnpmLockFile file ✂️...")
        pnpmLockFile.removeLocal()
    }

     private fun remote(): Remote {
         return (server ?: SshServer(hostSsh = host!!, userSsh = user, rootFolder = project.rootDir.toString())).remote(checkKnownHosts)
     }
     private fun Service.runSessions(action: RunHandler.() -> Unit) = run(delegateClosureOf(action))
     private fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) = session(*remotes, delegateClosureOf(action))


    private val statistic: MutableMap<String, Long> = mutableMapOf()

    private fun printDurationStatistic() {
        val top10LongestOperations = statistic.toList().sortedByDescending { (_, value) -> value }.take(10).toMap()
        println("\n\t ⏰ ${top10LongestOperations.size} longest operations (only longer than 1 sec will be printed):")
        var i = 1
        top10LongestOperations.forEach {
            val durationSec = MILLISECONDS.toSeconds(it.value)
            if (durationSec > 1) println("\t ${i++}. ⏱️ ${MILLISECONDS.toMinutes(it.value)} min, or $durationSec sec ( ${it.value} ms - ${it.key} )")
    } }
}


fun Project.registerScpTask() = tasks.register<online.colaba.Ssh>("scp")
val Project.scp: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>("scp"){
        description = "🚛 🚐 🚒 🚎 Deploy all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/nginx folder)"
        postgres = "postgres"
        backend = true
        newrelic = true
        nginx = true
        docker = true
        gradle = true
        static = true
        elastic = true
        broker = true
        monitoring = true

        frontend = true
//        frontendWhole = true // ⬅️⬅️⬅️
//        frontendClear = true
        kibana = false
        admin = false
        config = false
        withBuildSrc = false

        run = "cd ${project.name} && echo \$PWD"
    }

fun Project.registerSshTask() = tasks.register<online.colaba.Ssh>(sshGroup)
val Project.ssh: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>(sshGroup){
        description = "🛴 Template for SSH deploy. All props are set to `false`"
    }

fun Project.registerFrontTask() = tasks.register<online.colaba.Ssh>("sshFront")
val Project.sshFront: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>("sshFront"){
        frontend = true
//        frontendWhole = true // ⬅️⬅️⬅️
//        frontendClear = true
        description = "🏎 FRONTEND deploy."
    }

fun Project.registerJarsTask() = tasks.register<online.colaba.Ssh>("sshJars")
val Project.sshJars: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>("sshJars"){
        backend = true
        newrelic = true
        docker = true
        gradle = true
        description = "🚜 BACKENDs jars deploy"
    }

fun Project.registerPostgresTask() = tasks.register<online.colaba.Ssh>("sshPostgres")
val Project.sshPostgres: TaskProvider<online.colaba.Ssh>
    get() = tasks.named<online.colaba.Ssh>("sshPostgres"){
        postgres = "postgres"
        description = "🚙 Deploy POSTGRES"
    }

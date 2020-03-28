package online.colaba

const val defaultHost = "colaba.online"

const val frontendService = "frontend"
const val backendService = "backend"

const val staticDir = "static"
const val nginxService = "nginx"
const val postgresService = "postgres"

const val dockerPrefix = "docker"
const val removeGroup = "remove"

fun jarLibsFolder(folder: String = backendService) = "$folder/build/libs"
val backends = setOf(
    "mail",
    "chat",
    "common-lib",
    "gateway",
    "config-server",
    "eureka-server",
    "auth"
)

val userHomePath: String = System.getProperty("user.home")
val isWindows: Boolean = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun String.normalizeForWindows(): String = this.replace("\\", "/")
fun String.splitBySpace(): List<String> = this.replace("  ", " ").split(" ")

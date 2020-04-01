package online.colaba


var backendServices = setOf(
    "mail",
    "chat",
    "gateway",
    "config-server",
    "eureka-server",
    "auth"
)
val adminBackendServices = setOf("admin-server", "admin-client")

const val defaultHost = "colaba.online"

const val frontendService = "frontend"
const val backendService = "backend"

const val staticDir = "static"
const val nginxService = "nginx"
const val postgresService = "postgres"

fun jarLibsFolder(folder: String = backendService) = "$folder/build/libs"

val userHomePath: String = System.getProperty("user.home")
val isWindows: Boolean = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun String.normalizeForWindows(): String = this.replace("\\", "/")
fun String.splitBySpace(): List<String> = this.replace("  ", " ").split(" ")

fun Exception.shortStackTrace(searchKeyWord: String = "org.gradle") = apply {
    stackTrace = stackTrace.filter { it.className.contains(searchKeyWord) }.toTypedArray()
}

fun Exception.shortStackTraceWithPrint(searchKeyWord: String = "org.gradle") =
    shortStackTrace(searchKeyWord).apply { printStackTrace() }

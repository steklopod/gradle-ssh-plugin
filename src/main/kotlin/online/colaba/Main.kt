package online.colaba

const val DEFAULT_HOST = "colaba.online"


const val STATIC = "static"

const val NGINX = "nginx"
const val CHAT = "chat"
const val BACKEND = "backend"
const val POSTGRES = "postgres"
const val FRONTEND = "frontend"
const val ELASTIC = "elastic"
const val ELASTIC_DOCKER_DATA = "elastic-data"

val JAVA_JARS: MutableSet<String> = mutableSetOf(
    "auth",
    "card",
    "mail",
    CHAT,
    "gateway",
    "eureka-server"
)


//Optional:
const val ADMIN_SERVER = "admin-server"
const val CONFIG_SERVER = "config-server"


fun jarLibFolder(folder: String = BACKEND) = "$folder/build/libs"

val userHomePath: String = System.getProperty("user.home")
val isWindows: Boolean = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()


fun String.normalizeForWindows(): String = replace("\\", "/").replace("//", "/")
fun String.splitBySpace(): List<String> = replace("  ", " ").split(" ")

fun Exception.shortStackTrace(searchKeyWord: String = "org.gradle") = apply {
    stackTrace = stackTrace.filter { it.className.contains(searchKeyWord) }.toTypedArray()
}

fun Exception.shortStackTraceWithPrint(searchKeyWord: String = "org.gradle") =
    shortStackTrace(searchKeyWord).apply { printStackTrace() }

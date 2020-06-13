package online.colaba

const val DEFAULT_HOST = "colaba.online"

val JAVA_JARS: MutableSet<String> = mutableSetOf(
    "auth",
    "card",
    "mail",
    "chat",
    "gateway",
    "eureka-server"
)

const val STATIC = "static"

const val NGINX = "nginx"
const val BACKEND = "backend"
const val POSTGRES = "postgres"
const val FRONTEND = "frontend"

//Optional:
const val ADMIN_SERVER = "admin-server"
const val CONFIG_SERVER = "config-server"


fun jarLibsFolder(folder: String = BACKEND) = "$folder/build/libs"

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

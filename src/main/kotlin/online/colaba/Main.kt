package online.colaba

const val DEFAULT_HOST = "colaba.online"

const val POSTGRES  = "postgres"
const val STATIC    = "static"
const val NGINX     = "nginx"
const val BACKEND   = "backend"
const val FRONTEND  = "frontend"
const val CHAT      = "chat"
//Elastic:
const val ELASTIC             = "elastic"
const val ELASTIC_CERT_NAME   = "elastic-stack-ca.p12"
const val ELASTIC_DOCKER_DATA = "elastic-data"
//Optional:
const val ADMIN_SERVER  = "admin-server"
const val CONFIG_SERVER = "config-server"

val JAVA_JARS: Set<String> = mutableSetOf(
    "auth",
    "card",
    "mail",
    CHAT,
    "gateway",
    "eureka-server"
)

fun jarLibFolder(folder: String = BACKEND) = "$folder/build/libs"

val userHomePath: String        = System.getProperty("user.home")
val isWindows: Boolean          = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun String.normalizeForWindows(): String = replace("\\", "/").replace("//", "/")
fun String.splitBySpace(): List<String>  = replace("  ", " ").split(" ")

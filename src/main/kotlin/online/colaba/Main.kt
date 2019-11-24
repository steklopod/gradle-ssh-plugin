package online.colaba

object Main

const val frontendService = "frontend"
const val backendService = "backend"
const val nginxService = "nginx"

const val buildGroup = "build"
const val publishFront = "publishFront"

const val dockerPrefix = "docker"
const val dockerfile = "Dockerfile"
const val dockerignoreFile = ".dockerignore"
const val dockerComposeFile = "docker-compose.yml"
const val dockerComposedevFile = "docker-compose.dev.yml"


val userHomePath: String = System.getProperty("user.home")
val isWindows: Boolean = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun String.normalizeForWindows(): String = this.replace("\\", "/")
fun String.splitBySpace(): List<String> = this.replace("  ", " ").split(" ")

package online.colaba

import org.gradle.api.Project
import java.io.File

const val DEFAULT_HOST = "colaba.online"

const val STATIC    = "static"
const val NGINX     = "nginx"
const val FRONTEND  = "frontend"
//Elastic:
const val ELASTIC             = "elastic"
const val ELASTIC_CERT_NAME   = "elastic-stack-ca.p12"
const val ELASTIC_DOCKER_DATA = "elastic-data"

fun jarLibFolder(folder: String = "backend") = "$folder/build/libs"

val userHomePath: String        = System.getProperty("user.home")
val isWindows: Boolean          = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun String.normalizeForWindows(): String = replace("\\", "/").replace("//", "/")
fun String.splitBySpace(): List<String>  = replace("  ", " ").split(" ")
fun Project.localExists(directory: String) = File("${rootDir}/$name/$directory".normalizeForWindows()).exists()
//    .apply { if (this) println("Created SSH task for JVM backend [$name]") }

package online.colaba

import org.gradle.api.Project
import java.io.File

const val STATIC = "static"
const val NGINX  = "nginx"
//Elastic:
const val ELASTIC               = "elastic"
const val ELASTIC_CERT_NAME     = "elastic-stack-ca.p12"
const val ELASTIC_DOCKER_VULUME = "elastic-data"

const val postgresConfigFile   = "postgresql.conf"
const val postgresConfigFolder = "docker-entrypoint-initdb.d"

val userHomePath: String        = System.getProperty("user.home")
val isWindows: Boolean          = System.getProperty("os.name").toLowerCase().contains("windows")
val windowsPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun jarLibFolder(folder: String = "backend") = "$folder/build/libs"

fun String.normalizeForWindows(projName: String? = null): String {
    val replace = replace("\\", "/").replace("//", "/").replace("//", "/")
    return projName?.let {
        replace.replace("$projName/$projName", projName)
    } ?: replace
}

fun Project.localExists(directory: String): Boolean {
    val pathname = "${rootDir}/$name/$directory".normalizeForWindows().replace("$name/$name", name)
    return File(pathname).exists()
}

fun Project.computeHostFromGroup(): String {
    var projGroup = group.toString()
    if (projGroup.isEmpty() || !projGroup.contains(".")) {
        projGroup = subprojects.find { it.group.toString().isNotEmpty() && projGroup.contains(".") }?.group.toString()
        if (projGroup.isEmpty() || !projGroup.contains(".")) {
            System.err.println("Error when computing `host` from `project.group`. SET GROUP AS REVERSED HOST in your `build.gradle.kts`: \n\n1) example how to set remote host (for ALL tasks): \n\n\tgroup = \"online.colaba\"")
            System.err.println("\n2) another way how to set directly remote host (in EACH task): \n\ntasks { \n\tscp { \n\t\thost =  \"colaba.online\" \n\t} \n}")
            throw RuntimeException("Host and group is not set! Set at least one of them.")
        }
    }
    return projGroup.split(".").reversed().joinToString(".")
}

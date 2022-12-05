package online.colaba

import org.gradle.api.Project
import java.io.File

const val STATIC = "static"
const val NGINX  = "nginx"
//Elastic:
const val ELASTIC               = "elastic"
const val BROKER                = "broker"
const val ELASTIC_CERTS_FOLDER     = "certs"
const val ELASTIC_DOCKER_VOLUME = "elastic-data"

const val postgresConfigFile   = "postgresql.conf"
const val postgresConfigFolder = "docker-entrypoint-initdb.d"

val userHomePath: String   = System.getProperty("user.home")
val isWindows: Boolean     = System.getProperty("os.name").toLowerCase().contains("windows")
val cmdPrefix: List<String> = if (isWindows) listOf("cmd", "/c") else listOf()

fun jarLibFolder(folder: String = "backend") = "$folder/build/libs"

fun String.normalizeForWindows(): String = replace("\\", "/").replace("//", "/").replace("//", "/")

fun Project.localExists(directory: String): Boolean {
    val absolutePath = "${rootProject.projectDir.absolutePath}/$name/$directory".normalizeForWindows().replace("$name/$name", name)
    return File(absolutePath).exists()
}

fun Project.computeHostFromGroup(): String {
    println("Ok: Optional property `host` was not passed to gradle ssh plugin. Computing from gradle `project.group`...")
    var projGroup = group.toString()
    if (projGroup.isEmpty() || !projGroup.contains(".")) {
        projGroup = subprojects.find { it.group.toString().isNotEmpty() && projGroup.contains(".") }?.group.toString()
        if (projGroup.isEmpty() || !projGroup.contains(".")) {
            System.err.println("Error when computing `host` from `project.group`. SET GROUP AS REVERSED HOST in your `build.gradle.kts`: \n\n1) example how to set remote host (for ALL tasks): \n\n\tgroup = \"online.colaba\"")
            System.err.println("\n2) another way how to set directly remote host (in EACH task): \n\ntasks { \n\tscp { \n\t\thost = \"colaba.online\" \n\t} \n}")
            throw RuntimeException("Host and group is not set! Set at least one of them.")
        }
    }
    return projGroup.split(".").reversed().joinToString(".")
}



fun validateHost(host: String): String {
    val ipV4Regex = Regex("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$")
    if (host.count { it == '.' } > 2 && !ipV4Regex.matches(host)) throw RuntimeException("HOST (property in ssh gradle plugin) [$host] is NOT VALID IPv4")
    else {
        val domainNameRegex = Regex("^(((?!\\-))(xn\\-\\-)?[a-z0-9\\-_]{0,61}[a-z0-9]{1,1}\\.)*(xn\\-\\-)?([a-z0-9\\-]{1,61}|[a-z0-9\\-]{1,30})\\.[a-z]{2,}\$")
        if (!domainNameRegex.matches(host)) throw RuntimeException("Domain name (host property in ssh gradle plugin) [$host] is NOT VALID DOMAIN")
    }
    return host
}

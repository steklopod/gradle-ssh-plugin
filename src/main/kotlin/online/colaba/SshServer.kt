package online.colaba

import org.gradle.api.GradleException
import org.hidetake.groovy.ssh.connection.AllowAnyHosts
import org.hidetake.groovy.ssh.core.Remote
import java.io.File


data class SshServer(
    val hostSsh: String = DEFAULT_HOST, val userSsh: String = defaultUser, val idRsaPath: String = idRsaPath()
) {
    companion object {
        private const val defaultUser = "root"
        private const val rsaKeyName = "id_rsa"
        private val defaultRsaPath = "$userHomePath/.ssh/${rsaKeyName}".normalizeForWindows()

        fun idRsaPath(idRsaPath: String = rsaKeyName): String {
            val exists = File(idRsaPath).exists()
            val existsDefault = File(defaultRsaPath).exists()
            if (exists) println("> OK : [$rsaKeyName] found in root folder of project")
            else {
                if (existsDefault) return defaultRsaPath
                else throw SshException("You don't have [$defaultRsaPath] file. Or you can put [$rsaKeyName] file in root directory.")
            }
            return idRsaPath
        }
    }

    fun remote(checkKnownHosts: Boolean): Remote {
        val config: MutableMap<String, *> = mutableMapOf(
            "knownHosts" to AllowAnyHosts.instance,
            "host" to hostSsh,
            "user" to userSsh,
            "identity" to File(idRsaPath)
        )
        if (checkKnownHosts) {
            println("* If you don't want to scan [known_hosts] local file - set `checkKnownHosts = false` in gradle [ssh] task.")
            config.remove("knownHosts")
        }
        return Remote(config)
                         // .apply { host = hostSsh; user = userSsh; identity = File(idRsaPath) }
    }
}

class SshException(override val message: String) : GradleException()

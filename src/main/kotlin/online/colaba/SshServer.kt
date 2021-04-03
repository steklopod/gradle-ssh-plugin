package online.colaba

import org.hidetake.groovy.ssh.connection.AllowAnyHosts
import org.hidetake.groovy.ssh.core.Remote
import java.io.File


data class SshServer(
    val hostSsh: String = DEFAULT_HOST,
    val userSsh: String = defaultUser,
    val rootFolder: String? = null
) {

    fun remote(checkKnownHosts: Boolean): Remote {
        val config: MutableMap<String, *> = mutableMapOf(
            "knownHosts" to AllowAnyHosts.instance,
            "host" to hostSsh,
            "user" to userSsh,
            "password" to false,
            "ignoreError" to true,
            "agent" to true,
            "identity" to idRsaPath(rootFolder)
        )
        if (checkKnownHosts) {
            println("* If you don't want to scan [known_hosts] local file - set `checkKnownHosts = false` in gradle [ssh, publish] tasks.")
            config.remove("knownHosts")
        }
        return Remote(config)
        // .apply { host = hostSsh; user = userSsh; identity = File(idRsaPath) }
    }

    companion object {
        private const val defaultUser = "root"
        private const val rsaKeyName = "id_rsa"
        private val defaultRsaPath = "$userHomePath/.ssh/${rsaKeyName}".normalizeForWindows()

        fun idRsaPath(rootFolder: String?): String? = rootFolder?.let {
            rsaInProjectPath(it)
                ?: rsaInLocalSshFolderPath()
                ?: throw RuntimeException("You don't have [$defaultRsaPath] file. Put [$rsaKeyName] file in root directory.")
        }

        private fun rsaInProjectPath(rootFolder: String?): String? = rootFolder?.let {
            val location = "$it/id_rsa".normalizeForWindows()
            if (File(location).exists()) {
                println("⚡ OK ⚡ [$rsaKeyName] found in local ROOT OF PROJECT: [$location]⬅️")
                location
            } else {
                println("🚩🚩🚩 [$rsaKeyName] 🚨NOT FOUND🚨 in ⚡⚡⚡ROOT OF PROJECT⚡⚡⚡: [$location]⬅️")
                null
            }
        }

        private fun rsaInLocalSshFolderPath(): String? = if (File(defaultRsaPath).exists()) {
            println("⚡ OK ⚡ [$rsaKeyName] found in LOCAL DEFAULT [$defaultRsaPath]⬅️.")
            println("⚡ Move it in root folder of your project to allow SSH deploy in CI/CD")
            defaultRsaPath
        } else {
            println("⚡⚡⚡ [$rsaKeyName] 🚨NOT FOUND🚨 in ⚡⚡⚡LOCAL DEFAULT⚡⚡⚡ [$defaultRsaPath]⬅️.")
            null
        }
    }

}


package online.colaba

import java.io.File


data class SshServer(val hostSsh: String, val userSsh: String = defaultUser, val rootFolder: String) {

    companion object {
        private const val defaultUser = "root"
        private const val rsaKeyName = "id_rsa"
        private val defaultRsaPath = "$userHomePath/.ssh/${rsaKeyName}".normalizeForWindows()

        fun idRsaPath(rootFolder: String?): File = File(rootFolder?.let {
            rsaInProjectPath(it) ?: rsaInLocalSshFolderPath()
        } ?: throw RuntimeException("You don't have [$defaultRsaPath] file. Put [$rsaKeyName] file in root directory."))

        private fun rsaInProjectPath(rootFolder: String?): String? = rootFolder?.let {
            val location = "$it/id_rsa".normalizeForWindows()
            if (File(location).exists()) {
                println("\uD83D\uDC4C OK: [$rsaKeyName] key ⚡ found in local project folder (root of the current project) ⬅️\n")
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


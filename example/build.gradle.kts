plugins { id("online.colaba.ssh") version "1.0.3" }

tasks {
    ssh {
        host = "online.colaba"
        user = "user"

        frontendFolder = "client"
        backendFolder = "server"
        directory = "copy_me_to_remote"
    }
}

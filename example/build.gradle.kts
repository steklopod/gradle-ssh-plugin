plugins { id("online.colaba.ssh") version "1.2.30" }

tasks {
    ssh {
        host = "me.online"
        user = "user"

        frontendFolder = "client"
        backendFolder = "server"
        directory = "copy_me_to_remote"
    }
}

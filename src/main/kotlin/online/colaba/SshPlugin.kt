package online.colaba

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*


class SshPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        description = "SSH needed deploy-tasks"

        registerSshTask()
        registerDockerComposeTask()

        ssh {   }

        tasks {

            register("publish", Ssh::class) {
                frontend = true
                backend = true
                docker = true
                gradle = true
                nginx = true
                run = "cd ${project.name} && echo \$PWD"
                description = "Copy for all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/nginx folder)"
            }
            register("publishFront", Ssh::class) { frontend = true }
            register("publishBack", Ssh::class) { backend = true;  description = "Copy backend folder to remote server" }
            register("publishGradle", Ssh::class) { gradle = true; description = "Copy gradle needed files to remote server" }
            register("publishDocker", Ssh::class) { docker = true; description = "Copy docker needed files to remote server" }
            register("publishNginx", Ssh::class) { nginx = true;  description = "Copy nginx folder to remote server" }

            register("prune", Executor::class) { command = "$dockerPrefix system prune -fa"; description = "Remove unused docker data"; group = sshGroup }
            val removeBackAndFront by registering{ dependsOn(":$frontendService:$removeGroup"); finalizedBy(":$backendService:$removeGroup"); description = "Docker remove backend & frontend containers"; group = sshGroup }
            val removeAll by registering { dependsOn(":$nginxService:$removeGroup"); finalizedBy(removeBackAndFront); description = "Docker remove `nginx`, `backend` & `frontend` containers"; group = sshGroup }

            compose{   }
            val composeDev by registering(DockerCompose::class) { dependsOn(":$backendService:assemble"); isDev = true; description = "Docker compose up from `docker-compose.dev.yml` file after backend `assemble` task" }
            register("composeNginx", DockerCompose::class) { service = nginxService; description = "Docker compose up for nginx container" }
            register("composeBack", DockerCompose::class) { service = backendService; description = "Docker compose up for backend container" }
            register("composeFront", DockerCompose::class) { service = frontendService; description = "Docker compose up for frontend container" }
            register("recomposeAll") { dependsOn(removeAll); finalizedBy(compose); description = "Compose up after removing `nginx`, `frontend` & `backend` containers" }
            register("recomposeAllDev") { dependsOn(removeAll); finalizedBy(composeDev); description = "Compose up from `docker-compose.dev.yml` after removing `nginx`, `frontend` & `backend` containers" }
        }
    }
}

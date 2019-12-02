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

        compose{   }

        tasks {
            register(publishFront, Ssh::class) { frontend = true }

            register("publish", Ssh::class) {
                gradle = true
                frontend = true
                backend = true
                static = true
                docker = true
                nginx = true
                run = "cd ${project.name} && echo \$PWD"
                description = "Copy for all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/static/nginx folder)"
            }

            register("publishGradle", Ssh::class) { gradle = true; description = "Copy gradle needed files to remote server" }
            register("publishDocker", Ssh::class) { docker = true; description = "Copy docker needed files to remote server" }
            register("publishStatic", Ssh::class) { static = true; description = "Copy static folder to remote server" }
            register("publishBack", Ssh::class) { backend = true;  description = "Copy backend folder to remote server" }

            register("prune", Docker::class) { exec = "system prune -fa"; description = "Remove unused docker data" }
            val removeBackAndFront by registering(Docker::class) { dependsOn(":$frontendService:$removeGroup"); finalizedBy(":$backendService:$removeGroup"); description = "Docker remove backend & frontend containers" }
            val removeAll by registering(Docker::class) { dependsOn(":$nginxService:$removeGroup"); finalizedBy(removeBackAndFront); description = "Docker remove `nginx`, `backend` & `frontend` containers" }

            val composeDev by registering(DockerCompose::class) { dependsOn(":$backendService:assemble"); isDev = true; description = "Docker compose up from `docker-compose.dev.yml` file after `assemble` task" }
            register("composeNginx", DockerCompose::class) { service = nginxService; description = "Docker compose up for nginx container" }
            register("composeBack", DockerCompose::class) { service = backendService; description = "Docker compose up for backend container" }
            register("composeFront", DockerCompose::class) { service = frontendService; description = "Docker compose up for frontend container" }

            register("recomposeAll") { dependsOn(removeAll); finalizedBy(compose); description = "Compose up after removing `nginx`, `frontend` & `backend` containers" }
            register("recomposeAllDev") { dependsOn(removeAll); finalizedBy(composeDev); description = "Compose up from `docker-compose.dev.yml` after removing `nginx`, `frontend` & `backend` containers" }
        }
    }
}

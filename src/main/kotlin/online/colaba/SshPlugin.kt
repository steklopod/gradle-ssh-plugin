package online.colaba

import online.colaba.DockerCompose.Companion.dockerMainGroupName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*


class SshPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        description = "SSH needed deploy-tasks +++ Docker-compose for root "

        registerSshTask()
        registerCmdTask()
        registerDockerComposeTask()

        ssh {   }

        cmd {   }

        tasks {
            register("publish", Ssh::class) {
                frontend = true
                backend = true
                docker = true
                gradle = true
                nginx = true
                static = true
                postgres = true
                clearNuxt = true
                monolit = false

                admin = true

                run = "cd ${project.name} && echo \$PWD"
                description = "Copy for all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/nginx folder)"
            }
            register("publishFront", Ssh::class)  { frontend = true }
            register("publishBack", Ssh::class)   { monolit = true;  description = "Copy backend folder to remote server" }
            register("publishGradle", Ssh::class) { gradle = true; description = "Copy gradle needed files to remote server" }
            register("publishDocker", Ssh::class) { docker = true; description = "Copy docker needed files to remote server" }
            register("publishNginx", Ssh::class)  { nginx = true;  description = "Copy nginx folder to remote server" }

            register("pruneFront", Ssh::class)  { clearNuxt = true;  description = "Remove local [node_modules]" }
            register("prune", Cmd::class) { command = "docker system prune -fa"; description = "Remove unused docker data"; group = dockerMainGroupName(project.name) }

            val ps by registering (Cmd::class) { command = "docker ps"; description = "Print all containers"; group = dockerMainGroupName(project.name) }
            val stopAll by registering (Cmd::class) {dockerForEachSubproject(project, "stop", postgresService); description = "Docker stop all containers"; group = dockerMainGroupName(project.name) }
            val rm  by registering (Cmd::class) {
                description = "Docker remove all containers"; group = dockerMainGroupName(project.name)
                command = "docker rm -vf \$(docker ps -q)"
                finalizedBy(ps)
            }

            compose{   }
            val composeDev by registering(DockerCompose::class) { dependsOn(":$backendService:assemble"); isDev = true; description = "Docker compose up from `docker-compose.dev.yml` file after backend `assemble` task" }
            register("composeNginx", DockerCompose::class) { service = nginxService; description = "Docker compose up for nginx container" }
            register("composeFront", DockerCompose::class) { service = frontendService; description = "Docker compose up for frontend container" }
            register("composeBack", DockerCompose::class) { service = backendService; description = "Docker compose up for backend container" }

            register("recomposeAll") { dependsOn(rm); finalizedBy(compose); description = "Compose up after removing `nginx`, `frontend` & `backend` containers"; group = dockerMainGroupName(project.name) }
            register("recomposeAllDev") { dependsOn(rm); finalizedBy(composeDev); description = "Compose up from `docker-compose.dev.yml` after removing `nginx`, `frontend` & `backend` containers"; group = dockerMainGroupName(project.name) }
        }
    }

}

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

        ssh { }

        cmd {   }

        tasks {
            register("publish", Ssh::class) {
                description = "Copy for all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/nginx folder)"

                nginx = true
                docker = true
                gradle = true
                static = true
                backend = true
                frontend = true
                postgres = true

                admin = false
                clearNuxt = true

                monolit = false

//              run = "cd ${project.name} && echo \$PWD"
            }

            register("sshFront", Ssh::class)   { frontend = true;  description = "Copy [frontend] to remote server"  }
            register("sshMonolit", Ssh::class) { monolit = true;   description = "Copy [backend] to remote server" }
            register("sshBack", Ssh::class)    { backend = true;   description = "Copy all jars to remote server" }
            register("sshGradle", Ssh::class)  { gradle = true;    description = "Copy [gradle] needed files to remote server" }
            register("sshDocker", Ssh::class)  { docker = true;    description = "Copy [docker] needed files to remote server" }
            register("sshNginx", Ssh::class)   { nginx = true;     description = "Copy [nginx] to remote server" }
            register("sshPostgres", Ssh::class){ postgres = true;  description = "Copy [postgres] to remote server" }

            compose{   }

            register("clearFront", Ssh::class){ clearNuxt = true;  description = "Remove local [node_modules] & [.nuxt]" }
            register("prune", Cmd::class)     { command = "docker system prune -fa"; description = "Remove unused docker data"; group = dockerMainGroupName(project.name) }

            register("composeNginx", DockerCompose::class){ service = nginxService;    description = "Docker compose up for nginx container" }
            register("composeFront", DockerCompose::class){ service = frontendService; description = "Docker compose up for frontend container" }
            register("composeBack", DockerCompose::class) { service = backendService;  description = "Docker compose up for backend container" }

            val composeDev by registering(DockerCompose::class) { dependsOn(":$backendService:assemble"); isDev = true; description = "Docker compose up from `docker-compose.dev.yml` file after backend `assemble` task" }
            val ps by registering (Cmd::class) { command = "docker ps"; description = "Print all containers"; group = dockerMainGroupName(project.name) }
            val stopAll by registering (Cmd::class) {dockerForEachSubproject(project, "stop", postgresService); description = "Docker stop all containers"; group = dockerMainGroupName(project.name) }
            val rm  by registering (Cmd::class) {
                description = "Docker remove all containers"; group = dockerMainGroupName(project.name)
                command = "docker rm -vf \$(docker ps -q)"
                finalizedBy(ps)
            }

        }
    }

}

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

        compose{   }

        tasks {
            register("publish", Ssh::class) {
                description = "Copy for all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/nginx folder)"

                jars = JAVA_JARS

                nginx = true
                docker = true
                gradle = true
                static = true
                frontend = true
                postgres = true
                elastic = true

                clearNuxt = true
                withBuildSrc = false

                kibana = false
                admin = false
                config = false
                monolit = false
                run = "cd ${project.name} && echo \$PWD"
            }

            JAVA_JARS.forEach{ register("ssh-$it", Ssh::class){ directory = jarLibFolder(it); description = "Copy [${jarLibFolder(it)}] to remote server"  } }
            register("ssh-jars", Ssh::class)     { jars = JAVA_JARS; description = "Copy all {*.jars} to remote server" }
            register("ssh-$FRONTEND", Ssh::class){ frontend = true;  description = "Copy [$FRONTEND] jar to remote server" }
            register("ssh-$NGINX", Ssh::class)   { nginx = true;     description = "Copy [$NGINX] jar to remote server" }
            register("ssh-$POSTGRES", Ssh::class){ postgres = true;  description = "Copy [$POSTGRES] jar to remote server" }
            register("ssh-$BACKEND", Ssh::class) { monolit = true;   description = "Copy [$BACKEND] jar to remote server" }
            register("ssh-gradle", Ssh::class)   { gradle = true;    description = "Copy [gradle] needed files to remote server" }
            register("ssh-docker", Ssh::class)   { docker = true;    description = "Copy [docker] needed files to remote server" }

            JAVA_JARS.forEach{ register("compose-$it", DockerCompose::class){ service = it;  description = "Docker compose up for [$it] container" } }
            register("compose-$BACKEND", DockerCompose::class) { service = BACKEND; description = "Docker compose up for [$BACKEND] container" }
            register("compose-$FRONTEND", DockerCompose::class){ service = FRONTEND; description = "Docker compose up for [$FRONTEND] container" }
            register("compose-$NGINX", DockerCompose::class)   { service = NGINX;    description = "Docker compose up for [$NGINX] container" }
            register("compose-$POSTGRES", DockerCompose::class){ service = POSTGRES; description = "Docker compose up for [$POSTGRES] container" }
            register("compose-$ELASTIC", DockerCompose::class) { service = ELASTIC;  description = "Docker compose up for [$ELASTIC] container" }

            register("clear-$FRONTEND", Ssh::class){ clearNuxt = true;  description = "Remove local [node_modules] & [.nuxt]" }
            register("prune", Cmd::class){ command = "docker system prune -fa"; description = "Remove unused docker data"; group = dockerMainGroupName(project.name) }

            val ps by registering (Cmd::class) { command = "docker ps"; description = "Print all containers"; group = dockerMainGroupName(project.name) }

            val composeDev by registering(DockerCompose::class) { dependsOn(":$BACKEND:assemble"); isDev = true; description = "Docker compose up from `docker-compose.dev.yml` file after backend `assemble` task" }
            val stopAll by registering (Cmd::class) {dockerForEachSubproject(project, "stop", POSTGRES); description = "Docker stop all containers"; group = dockerMainGroupName(project.name) }
            val rm  by registering (Cmd::class) {
                command = "docker rm -vf \$(docker ps -q)"; description = "Docker remove all containers"; group = dockerMainGroupName(project.name)
                finalizedBy(ps)
            }


        }
    }

}

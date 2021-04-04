package online.colaba

import online.colaba.DockerCompose.Companion.dockerMainGroupName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering
import java.io.File

class SshPlugin : Plugin<Project> { override fun apply(project: Project): Unit = project.run {
description = "SSH needed deploy-tasks. Docker-friendly"

registerSshTask(); registerJarsTask(); registerFrontTask(); registerPostgresTask()
ssh { }
sshJars { }
sshFront { }
sshPostgres{ }

registerCmdTask(); registerDockerComposeTask()
cmd { }
compose{ }


val (backendJARs, wholeFolder) = subprojects
    .filter { !name.endsWith("lib") && !name.contains("postgres")  && !name.contains("front")}
    .partition { it.localExists("src/main") || it.localExists("build/libs") }


tasks {
    backendJARs.forEach { register("ssh-${it.name}", Ssh::class) { directory = jarLibFolder(it.name); description = "Copy backend [${jarLibFolder(it.name)}] jar to remote server" } }
    wholeFolder.forEach { register("ssh-${it.name}", Ssh::class) { directory = it.name; description = "Copy whole folder [${it.name}] to remote server" } }

    register("ssh-docker", Ssh::class){ docker = true; description = "Copy [docker] needed files to remote server" }
    register("ssh-gradle", Ssh::class){ gradle = true; description = "Copy [gradle] needed files to remote server" }

    register("publish", Ssh::class) {
        description = "Copy for all projects to remote server: gradle/docker needed files, backend .jar distribution, frontend/nginx folder)"
        postgres = "postgres"
        frontend = true
        clearNuxt = true
        backend = true
        nginx = true
        docker = true
        gradle = true
        static = true
        elastic = true

        kibana = false
        admin = false
        config = false
        withBuildSrc = false

        run = "cd ${project.name} && echo \$PWD"
    }

    register("clear-$FRONTEND", Ssh::class){ clearNuxt = true;  description = "Remove local [node_modules] & [.nuxt]" }

    // Docker helpers
    subprojects.forEach { register("compose-${it.name}", DockerCompose::class){ service = it.name; description = "Docker compose up for [${it.name}] container" } }

    val ps by registering (Cmd::class) { command = "docker ps";   description = "Print all containers"; group = dockerMainGroupName(project.name) }
    val stopAll by registering (Cmd::class) { dockerForEachSubproject(project, "stop"); description = "Docker stop all containers"; group = dockerMainGroupName(project.name) }
    register("prune", Cmd::class)  { command = "docker system prune -fa";        description = "Remove unused docker data"; group = dockerMainGroupName(project.name) }
    register("rm-all", Cmd::class) { command = "docker rm -vf $(docker ps -q)";  description = "Docker remove all containers"; group = dockerMainGroupName(project.name); dependsOn(stopAll); finalizedBy(ps) }

} } }

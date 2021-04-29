package online.colaba

import online.colaba.DockerCompose.Companion.dockerMainGroupName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering

class SshPlugin : Plugin<Project> { override fun apply(project: Project): Unit = project.run {
description = "SSH needed deploy-tasks. Docker-friendly"

registerSshTask(); registerJarsTask(); registerFrontTask(); registerPostgresTask()
ssh { }
sshJars { }
sshFront { }
sshPostgres{ }

registerCmdTask(); registerDockerComposeTask(); registerSchemaTask()
cmd { }
schema{ }
compose{ }


val (backendJARs, wholeFolder) = subprojects
    .filter { !name.endsWith("lib") && !name.contains("postgres")  && !name.contains("front")}
    .partition { it.localExists("src/main") || it.localExists("build/libs") }


tasks {
    backendJARs.map{it.name}.forEach { register("ssh-${it}", Ssh::class) { directory = jarLibFolder(it); description = "Copy backend [${jarLibFolder(it)}] jar to remote server" } }
    wholeFolder.map{it.name}.filter{!it.contains("static")}.forEach { register("ssh-$it", Ssh::class) { directory = it; description = "Copy whole folder [$it] to remote server" } }

    register("ssh-docker", Ssh::class){ docker = true; description = "Copy [docker] needed files to remote server" }
    register("ssh-gradle", Ssh::class){ gradle = true; description = "Copy [gradle] needed files to remote server" }

    val offVolumes by registering (Cmd::class) { command = "docker-compose down -v";   description = "Delete the volume between runs"}
    val composeDetach by registering (Cmd::class) { command = "docker-compose up -d";   description = "docker-compose up -d"}
    register("ssh-static-force", Ssh::class){ dependsOn(offVolumes); staticOverride = true; finalizedBy(composeDetach); description = "Force copy [static] with override to remote server"}

    register("scp", Ssh::class) {
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

    register("clear-frontend", Ssh::class){ clearNuxt = true;  description = "Remove local [node_modules] & [.nuxt]" }

    // DOCKER helpers
    subprojects.forEach { register("compose-${it.name}", DockerCompose::class){ service = it.name; description = "Docker compose up for [${it.name}] container" } }

    val ps      by registering (Cmd::class){ command = "docker ps"; description = "Print all containers to console output"; group = dockerMainGroupName(project.name) }
    val stopAll by registering (Cmd::class) { composeForEachSubproject(project, "stop"); description = "Docker stop all containers"; group = dockerMainGroupName(project.name) }
    val rm      by registering (Cmd::class) { dependsOn(stopAll); composeForEachSubproject(project, "rm -f"); description = "Docker remove all containers"; group = dockerMainGroupName(project.name) }
    val prune   by registering (Cmd::class) { command = "docker system prune -fa"; description = "Remove unused docker data"; group = dockerMainGroupName(project.name); finalizedBy(ps) }
    val rmStaticVolume by registering (Cmd::class) { command = "docker volume rm -f ${project.name}_static";  description = "Removing static volume"; group = dockerMainGroupName(project.name);
        dependsOn(offVolumes)
        finalizedBy(prune)
    }

    register("rm-all", Cmd::class) { description = "Docker remove all containers"; group = dockerMainGroupName(project.name);
        dependsOn(rm)
        finalizedBy(rmStaticVolume)
    }
} } }

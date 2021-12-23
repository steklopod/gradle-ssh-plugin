package online.colaba

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering

class SshPlugin : Plugin<Project> { override fun apply(project: Project): Unit = project.run {
description = "SSH needed deploy-tasks. Docker-friendly"


registerSshTask(); registerScpTask(); registerJarsTask(); registerFrontTask(); registerPostgresTask()
ssh { }
scp { }
sshJars { }
sshFront { }
sshPostgres { }

registerCmdTask(); registerDckrTask(); registerDockerComposeTask(); registerDockerComposeUpTask();
cmd { }
dckr { }
compose { }
composeUp { }

val (backendJARs, wholeFolder) = subprojects
    .filter { !name.endsWith("lib") && !name.contains("postgres") && !name.contains("front")}
    .partition { it.localExists("src/main") || it.localExists("build/libs") }


tasks {
    backendJARs.map{it.name}.forEach { register("ssh-${it}", Ssh::class) { directory = jarLibFolder(it); description = "Copy backend [${jarLibFolder(it)}] jar to remote server" } }
    wholeFolder.map{it.name}.filter{!it.contains("static")}.forEach { register("ssh-$it", Ssh::class) { directory = it; description = "Copy whole folder [$it] to remote server" } }

    register("ssh-docker", Ssh::class){ docker = true; description = "Copy [docker] needed files to remote server" }
    register("ssh-gradle", Ssh::class){ gradle = true; description = "Copy [gradle] needed files to remote server" }
    register("ssh-static-force", Ssh::class){ staticOverride = true; finalizedBy(compose); description = "Force copy [static] with override to remote server"}

    register("clear-frontend", Ssh::class){ frontendClear = true;  description = "Remove local [node_modules] & [.nuxt]" }

    // DOCKER
    subprojects.forEach {
        register("compose-${it.name}", DockerComposeUp::class){ service = it.name; description = "Docker compose up for [${it.name}] container" }
    }

    val ps      by registering (Dckr::class) { exec = "ps"; description = "Print all containers to console output" }

    val down by registering (DockerCompose::class) { exec = "down -fvs";   description = "tops containers and removes containers, networks, volumes, and images created by up"}

    val prune   by registering (Dckr::class) { exec = "system prune -fa"; description = "Remove unused docker data"; finalizedBy(ps) }
    val networkPrune by registering (Dckr::class) { exec ="network prune -f"; description = "Remove unused docker networks" }

    val rmPostgresVolume by registering (Dckr::class) { exec = "volume rm -f ${project.name}_postgres-data"; description = "Remove volume postgres"}
    val rmElasticVolume by registering (Dckr::class) { exec = "volume rm -f ${project.name}_elastic-data"; description = "Remove volume elastic"}
    val volumesRm by registering (Dckr::class) { dependsOn(rmElasticVolume); finalizedBy(rmPostgresVolume); exec = "volume rm -f ${project.name}_backups"; description = "Remove volume backups"}
    val volumePrune by registering (Dckr::class) { dependsOn(down); finalizedBy(volumesRm); description = "Docker down & Volume prune"}
    val rmStaticVlm by registering (Dckr::class) { dependsOn(volumePrune); exec ="volume rm -f ${project.name}_static"; finalizedBy(networkPrune)}

    val stopAll by registering (DockerCompose::class) { exec ="down -v"; description = "Stop all docker containers" }
    register("pruneAll", DockerCompose::class) {
        dependsOn(stopAll)
        exec ="rm -f"
        description = "Docker remove all containers & volumes & networks & images"
        finalizedBy(prune)
    }

} } }

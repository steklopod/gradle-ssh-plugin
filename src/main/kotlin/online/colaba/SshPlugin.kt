package online.colaba

import online.colaba.DockerComposeUp.Companion.dockerMainGroupName
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

registerCmdTask(); registerDckrTask(); registerDockerComposeTask(); registerDockerComposeUpTask();
cmd { }
dckr{ }
compose{ }
composeUp{ }


val (backendJARs, wholeFolder) = subprojects
    .filter { !name.endsWith("lib") && !name.contains("postgres")  && !name.contains("front")}
    .partition { it.localExists("src/main") || it.localExists("build/libs") }


tasks {
    backendJARs.map{it.name}.forEach { register("ssh-${it}", Ssh::class) { directory = jarLibFolder(it); description = "Copy backend [${jarLibFolder(it)}] jar to remote server" } }
    wholeFolder.map{it.name}.filter{!it.contains("static")}.forEach { register("ssh-$it", Ssh::class) { directory = it; description = "Copy whole folder [$it] to remote server" } }

    register("ssh-docker", Ssh::class){ docker = true; description = "Copy [docker] needed files to remote server" }
    register("ssh-gradle", Ssh::class){ gradle = true; description = "Copy [gradle] needed files to remote server" }
    register("ssh-static-force", Ssh::class){ staticOverride = true; finalizedBy(compose); description = "Force copy [static] with override to remote server"}

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

    // DOCKER
    subprojects.forEach { register("compose-${it.name}", DockerComposeUp::class){ service = it.name; description = "Docker compose up for [${it.name}] container" } }

    val ps      by registering (Dckr::class) { exec = "ps"; description = "Print all containers to console output"; group = dockerMainGroupName(project.name) }

    val down by registering (DockerCompose::class) { exec = "down -fvs";   description = "tops containers and removes containers, networks, volumes, and images created by up"}

    val prune   by registering (Dckr::class) { exec = "system prune -fa"; description = "Remove unused docker data"; group = dockerMainGroupName(project.name); finalizedBy(ps) }
    val networkPrune by registering (Dckr::class) { exec ="network prune -f"; description = "Remove unused docker networks"; group = dockerMainGroupName(project.name) }
    //TODO
    val volumePr by registering (Dckr::class)    { rmVolumes(); description = "Remove unused docker volumes 2"; group = dockerMainGroupName(project.name) }
    val volumePrune by registering (Dckr::class) { dependsOn(down); finalizedBy(volumePr); description = "Docker down & Volume prune"; group = dockerMainGroupName(project.name) }
    val rmStaticVlm by registering (Dckr::class) { dependsOn(volumePrune); exec ="volume rm -f ${project.name}_static"; finalizedBy(networkPrune); group = dockerMainGroupName(project.name); }

    register("rm-all", DockerCompose::class) { exec ="rm -fvs"; description = "Docker remove all containers & volumes & networks"; group = dockerMainGroupName(project.name);
        finalizedBy(rmStaticVlm)
    }


} } }

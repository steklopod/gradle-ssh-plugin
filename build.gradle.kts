plugins {
    `kotlin-dsl`
    id("org.sonarqube") version "3.1.1"
    id("com.gradle.plugin-publish") version "0.14.0"
    id("com.github.ben-manes.versions") version "0.38.0"
}
val pluginsVersion = "1.7.38"
description = "Easy SCP deploy gradle needed tasks"
version = pluginsVersion
group = "online.colaba"

repositories{ mavenLocal(); mavenCentral() }

val sshPlugin = "sshPlugin"
gradlePlugin { plugins { create(sshPlugin) {
    id = "$group.ssh"; implementationClass = "$group.SshPlugin"
    description = "Ssh needed tasks for FTP deploy (SCP): all you need for easy deployment"
} } }
pluginBundle {
    website = "https://github.com/steklopod/gradle-ssh-plugin"
    vcsUrl = "https://github.com/steklopod/gradle-ssh-plugin.git"
    (plugins) { sshPlugin {
        displayName = "SSH task for easy deploy"
        description = "SSH task for easy deploy"
        tags = listOf("ssh", "deploy", "CI/CD", "sftp", "ftp", "docker", "docker-compose")
        version = pluginsVersion
    }
} }

dependencies {
    implementation("org.hidetake:groovy-ssh:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC")
}

tasks { compileKotlin { kotlinOptions { jvmTarget = "15"; useIR = true } } }

defaultTasks("clean", "assemble", "publishPlugins")

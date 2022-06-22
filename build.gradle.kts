plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.0.0-rc-3"
//    id("org.sonarqube") version "3.4.0.2513"
//    id("com.github.ben-manes.versions") version "0.42.0"
}
val pluginsVersion = "1.8.19"
version = pluginsVersion
group = "online.colaba"
description = "🚎 Deploy your multi-module gradle project distribution by ssh. 🚐 Easy SCP deploy gradle needed tasks."

repositories{ mavenLocal(); mavenCentral() }

val sshPlugin = "sshPlugin"
gradlePlugin { plugins { create(sshPlugin) {
    id = "$group.ssh"; implementationClass = "$group.SshPlugin"
    description = "🚐 Deploy your multi-module gradle project distribution by SSH (+ 🐳 Docker helpers tasks)"
    displayName = "SSH task for easy deploy to remote server"
} } }
pluginBundle {
    website = "https://github.com/steklopod/gradle-ssh-plugin"
    vcsUrl = "https://github.com/steklopod/gradle-ssh-plugin.git"
    tags = listOf("ssh", "scp", "deploy", "CI/CD", "sftp", "ftp", "docker", "docker-compose")
 }

dependencies {
    implementation("org.hidetake:groovy-ssh:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    testImplementation(platform("org.junit:junit-bom:5.9.0-M1"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}

tasks { test { useJUnitPlatform() } }

defaultTasks("clean", "assemble", "publishPlugins")

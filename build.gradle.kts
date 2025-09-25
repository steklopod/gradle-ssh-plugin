plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.0.0"
    id("com.github.ben-manes.versions") version "0.53.0"
}
val pluginsVersion = "2.0.1"
version = pluginsVersion
group = "online.colaba"
description = "🚎 Deploy your multi-module gradle project by ssh. 🚐 Easy SCP deploy tasks."

repositories { mavenCentral() }

val sshPlugin = "sshPlugin"
gradlePlugin {
    plugins {
        create(sshPlugin) {
            id = "$group.ssh"; implementationClass = "$group.SshPlugin"
            description = "🚐 SCP: deploy your multi-module gradle project distribution by SSH (+ 🐳 Docker helpers tasks)"
            displayName = "SCP tasks for easy deploy to remote server via ssh"

            tags.set(listOf("ssh", "scp", "deploy", "CI/CD", "sftp", "ftp", "docker", "docker-compose"))
            website.set("https://github.com/steklopod/gradle-ssh-plugin")
            vcsUrl.set("https://github.com/steklopod/gradle-ssh-plugin.git")
} } }

dependencies {
    implementation("org.hidetake:groovy-ssh:2.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(platform("org.junit:junit-bom:6.0.0-RC3"))
    testImplementation("org.junit.jupiter", "junit-jupiter-engine")
    testImplementation("org.junit.jupiter", "junit-jupiter-api")
}

defaultTasks("clean", "assemble", "publishPlugins")

kotlin { jvmToolchain(24) }
java { sourceCompatibility = JavaVersion.VERSION_24; targetCompatibility = JavaVersion.VERSION_24 }



plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.1.1"
    id("com.github.ben-manes.versions") version "0.54.0"
}
val pluginsVersion = "2.0.9"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    val junitVersion = "6.1.0"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
}

defaultTasks("clean", "assemble", "publishPlugins")

kotlin { jvmToolchain(25) }
java { sourceCompatibility = JavaVersion.VERSION_25; targetCompatibility = JavaVersion.VERSION_25 }

tasks.test { useJUnitPlatform() }

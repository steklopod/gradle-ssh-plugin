plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.0"
    id("com.github.ben-manes.versions") version "0.46.0"
}
val pluginsVersion = "1.9.1"
version = pluginsVersion
group = "online.colaba"
description = "🚎 Deploy your multi-module gradle project by ssh. 🚐 Easy SCP deploy gradle needed tasks."

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
    implementation("org.hidetake:groovy-ssh:2.11.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter", "junit-jupiter-engine")
    testImplementation("org.junit.jupiter", "junit-jupiter-api")
}

defaultTasks("clean", "assemble", "publishPlugins")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "17" } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }


plugins {
    `kotlin-dsl`
    val kotlin = "1.4.31"
    kotlin("jvm") version kotlin
    id("org.sonarqube") version "3.1.1"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("com.github.ben-manes.versions") version "0.36.0"
}

val pluginsVersion = "1.3.26"
val sshPlugin = "sshPlugin"
description = "Easy deploy gradle needed tasks"
version = pluginsVersion
group = "online.colaba"

repositories{ mavenLocal(); mavenCentral(); jcenter() }


gradlePlugin {
    plugins {
        create(sshPlugin) {
            id = "$group.ssh"; implementationClass = "$group.SshPlugin"
            description = "Ssh needed tasks for FTP deploy: all you need for easy deployment"
        }
    }

}

pluginBundle {
    website = "https://github.com/steklopod/gradle-ssh-plugin"
    vcsUrl = "https://github.com/steklopod/gradle-ssh-plugin.git"

    (plugins) {
        sshPlugin {
            displayName = "SSH task for easy deploy"
            description = "SSH task for easy deploy"
            tags = listOf("ssh", "deploy", "sftp", "ftp", "docker", "docker-compose")
            version = pluginsVersion
        }
    }
}

dependencies {
    implementation("net.sf.proguard:proguard-gradle:6.3.0beta1")
    implementation("org.hidetake:groovy-ssh:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

kotlinDslPluginOptions { experimentalWarning.set(false) }

tasks {
    val java = "15"
    compileKotlin { kotlinOptions { jvmTarget = java }; sourceCompatibility = java; targetCompatibility = java }
}

defaultTasks("clean", "assemble", "publishPlugins")

import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.10.1"
    id ("org.sonarqube") version "2.8"
}

val pluginsVersion = "0.1.4"
description = "EASY-DEPLOY gradle needed tasks"
version = pluginsVersion
group = "online.colaba"

repositories { mavenLocal(); mavenCentral() }

gradlePlugin {
    plugins {
        val sshPlugin by registering {
            id = "online.colaba.ssh"; implementationClass = "online.colaba.Ssh"
            description = "Ssh needed tasks for FTP deploy"
        }
    }

}

pluginBundle {
    website = "https://github.com/steklopod/gradle-ssh-plugin"
    vcsUrl = "https://github.com/steklopod/gradle-ssh-plugin.git"

    (plugins) {
        "sshPlugin" {
            displayName = "\uD83D\uDEE1️ SSH plugin for FTP deployment"
            tags = listOf("ssh", "kotlin", "deploy", "sftp", "ftp", "\uD83E\uDD1F\uD83C\uDFFB")
            version = pluginsVersion
        }

    }
}

dependencies {
    implementation("org.hidetake:groovy-ssh:2.10.1")
}

configure<JavaPluginConvention> { sourceCompatibility = VERSION_11; targetCompatibility = VERSION_11 }

tasks {
    withType<Wrapper> { gradleVersion = "6.0" }
    withType<KotlinCompile> { kotlinOptions { jvmTarget = "11" } }
}

sonarqube {
    properties {
        property ("sonar.projectKey", "steklopod_gradle-ssh-plugin")
    }
}
defaultTasks("tasks", "publishPlugins")


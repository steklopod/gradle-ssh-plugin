import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.10.1"
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
    website = "https://github.com/steklopod/gradle-ssh-plugin.git"
    vcsUrl = "https://github.com/steklopod/gradle-docker-plugin"

    (plugins) {
        "sshPlugin" {
            displayName = "\uD83D\uDEE1️ FTP deploy ssh-plugin gradle tasks\n" +
                    "Tasks: publishBack, publishFront, publish, ssh.\n"
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

defaultTasks("tasks", "publishPlugins")


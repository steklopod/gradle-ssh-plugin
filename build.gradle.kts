plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.11.0"
    id("org.sonarqube") version "2.8"
}


val pluginsVersion = "1.2.16"
val sshPlugin = "sshPlugin"
description = "EASY-DEPLOY gradle needed tasks"
version = pluginsVersion
group = "online.colaba"

repositories { mavenLocal(); mavenCentral() }


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
            tags = listOf("ssh", "deploy", "sftp", "ftp", "docker", "docker-compose")
            version = pluginsVersion
        }
    }
}

dependencies {
    implementation("org.hidetake:groovy-ssh:2.10.1")
}

kotlinDslPluginOptions { experimentalWarning.set(false) }

tasks {
    val java = "11"
    compileKotlin { kotlinOptions { jvmTarget = java }; sourceCompatibility = java; targetCompatibility = java }
}

defaultTasks("tasks", "publishPlugins")

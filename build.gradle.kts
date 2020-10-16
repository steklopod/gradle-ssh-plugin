plugins {
    `kotlin-dsl`
    id("org.sonarqube") version "3.0"
    id("com.gradle.plugin-publish") version "0.12.0"
    id("com.github.ben-manes.versions") version "0.33.0"
}

val pluginsVersion = "1.3.16"
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
    implementation("org.hidetake:groovy-ssh:2.10.1")
}

kotlinDslPluginOptions { experimentalWarning.set(false) }

tasks {
    val java = "11"
    compileKotlin { kotlinOptions { jvmTarget = java }; sourceCompatibility = java; targetCompatibility = java }
}

defaultTasks("clean", "assemble", "publishPlugins")

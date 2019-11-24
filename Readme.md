# Gradle `ssh` plugin  [![Build Status](https://travis-ci.org/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.org/steklopod/gradle-ssh-plugin)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

🛡️ Gradle `ssh` plugin for root multi-project

> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

### Easy to use
```kotlin
        // run command
        ssh {
            host = "online.colaba"; user = "user"
            
            directory = "frontend"
        }
        // copy directory
        ssh {
            host = "online.colaba"; user = "user"

            run = "echo \$PWD"
        }
```


### Quick start (`1 step only`)
In your `build.gradle.kts` file:
```kotlin
plugins {
     id("online.colaba.ssh") version "0.1.4"
}
```

### Execute by FTP 🎯
```shell script
gradle ssh
```

### Available gradle tasks for `ssh` plugin`:

> Name of service for all tasks equals to ${project.name} 

* `ssh` - send by ftp
* `publishGradle` - copy gradle's files
* `publishDocker` - copy docker's files
* `publishStatic` - copy static folder
* `publishBack` - copy backend's distribution `*.jar`-file


### Customization

1. Create custom task in 
```kotlin
        register("customSshTask", Ssh::class) {
            host = "online.colaba"
            user = "user"
            gradle = true
            frontend = true
            backend = true
            static = true
            docker = true
            nginx = true
            run = "cd ${project.name} && echo \$PWD"
        }
```
2. Call this task
```shell script
gradle customSshTask
```


> By default you have preconfigured profiles: in `ssh` task - all disabled, in `publish` task - all enabled.
```shell script
 project
    |-backend/
      | - build/libs/*.jar
    |-frontend/
    |-nginx/
```
You can customize this properties:
```kotlin
        ssh {
            host = "online.colaba"
            user = "user"
            frontendFolder = "client"
            backendFolder = "server"
            directory = "copy_me_to_remote"
        }
```

> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

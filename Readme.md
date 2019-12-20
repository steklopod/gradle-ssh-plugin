# Gradle `ssh` plugin  [![Build Status](https://travis-ci.com/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.com/steklopod/gradle-ssh-plugin)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

🛡️ Gradle `ssh` plugin for easy & quick deploy

#### Quick start
1. In your `build.gradle.kts` file:

```kotlin
plugins {
     id("online.colaba.ssh") version "1.0.1"
}

//Copy local project distribution folder into remote ~/{project.name}/ and print:
ssh {
    host = "online.colaba"; user = "user"   
    directory = "distribution"
    run = "ls -a"
}
```
> you must to have `id_rsa` private key (on your local machine: `{user.home}/.ssh/id_rsa`) to use this plugin.

### Execute by FTP 🎯
```shell script
gradle ssh
```

### Available gradle tasks for `ssh` plugin`:

> Name of service for all tasks equals to ${project.name} 

* `ssh`, `publish` - send by ftp, execute remote commands
* `publishBack` - copy backend's distribution `*.jar`-file
* `publishFront` - copy frontend folder
* `publishGradle` - copy gradle's needed files
* `publishDocker` - copy docker's files
* `publishNginx` - copy nginx folder
* `prune` - remove unused docker data

#### Custom task

1. Register new task in `build.gradle.kts`
```kotlin
        register("customSshTask", Ssh::class) {
            host = "online.colaba"
            user = "user"
            gradle = true
            frontend = true
            backend = true
            docker = true
            nginx = true
            run = "cd ${project.name} && echo \$PWD"
        }
```
2. Call this task
```shell script
gradle customSshTask
```
___
##### Preconfigured tasks

By default you have preconfigured profiles tasks: 
* `ssh` - all disabled  by default (**false**)
* `publish` - frontend, backend, docker folders will be copied (**true**)

You can customize this properties:
```kotlin
        ssh {
            host = "online.colaba"
            user = "user"
            frontendFolder = "client"
            backendFolder = "server"
            directory = "copy_me_to_remote"
            nginx = true
            docker = false
        }
```
Project's structure example
```shell script
 project
    |-backend/
      | - [build/libs]/*.jar
      | - Dockerfile
      | - Dockerfile.dev
      | - docker-compose.yml
      | - docker-compose.dev.yml
    |-frontend/
    |-nginx/
```
> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

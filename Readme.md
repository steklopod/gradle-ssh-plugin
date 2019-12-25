## 🛡️ Gradle `ssh` plugin for easy & quick deploy  [![Build Status](https://travis-ci.com/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.com/steklopod/gradle-ssh-plugin) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

#### Quick start
1. You must have `id_rsa` private key (on your local machine: `{user.home}/.ssh/id_rsa`) to use this plugin

2. In your `build.gradle.kts` file

```kotlin
plugins {
     id("online.colaba.ssh") version "1.0.4"
}

ssh {
    host = "hostexample.com"; user = "user"   
    directory = "distribution"
    run = "ls -a"
}
```

### Execute by FTP with SSH 🎯
```shell script
gradle ssh
```
> This tasks will copy local project **distribution** folder into remote **~/{project.name}/** and print it

### Available gradle tasks for `ssh` plugin:

Send by `ftp` with `ssh` (copy from local to remote server):
1. `publishBack` - copy **backend** distribution `*.jar`-file
2. `publishFront` - copy **frontend** folder
3. `publishNginx` - copy **nginx** folder
4. `publishGradle` - copy **gradle** needed files
5. `publishDocker` - copy **docker** files

All this tasks **includes** in 1 task:

* `publish` - all enabled  by default (**true**)

All this tasks **excluded** in 1 task:
* `ssh` task, where all disabled  by default (**false**) but can be included manually.

Other tasks:

* `compose` - docker compose up all docker-services with recreate and rebuild
* `composeDev` - docker compose up all docker-services with recreate and rebuild from `docker-compose.dev.yml` file
* `composeNginx`, `composeBack`, `composeFront` - docker compose up with recreate and rebuild
* `recomposeAll` - docker compose up after removing `nginx`, `frontend` & `backend` containers
* `recomposeAllDev` - docker compose up after removing `nginx`, `frontend` & `backend` containers `docker-compose.dev.yml` file

* `prune` - remove unused docker data
* `removeBackAndFront` - remove **backend**, **frontend** containers
* `removeAll` - remove **nginx**, **frontend**, **backend** containers 

> Name of service for all tasks equals to ${project.name} 

#### Customization

1. Register new task in your `build.gradle.kts`:
```kotlin
        register("customSshTask", Ssh::class) {
            host = "hostexample.com"
            user = "root"
            gradle = true
            frontend = true
            backend = true
            docker = true
            nginx = true
            run = "cd ${project.name} && echo \$PWD"
        }
```
2. Run this task:
```shell script
gradle customSshTask
```
___
#### Preconfigured tasks

By default you have preconfigured profiles tasks: 
* `ssh` - all disabled  by default (**false**)
* `publish` - all enabled  by default (**true**)

You can customize this properties:
```kotlin
        ssh {
            host = "hostexample.com"
            user = "root"
            frontendFolder = "client"
            backendFolder = "server"
            directory = "copy_me_to_remote"
            nginx = true
        }
```
Project's structure example
```shell script
 project
    |-[backend]/
      | - [build/libs]/*.jar
      | - Dockerfile
      | - Dockerfile.dev
      | - docker-compose.yml
      | - docker-compose.dev.yml
      |-[gradle]/
        | - ...
      | - gradlew
      | - gradlew.bat
      | - ...
    |-[frontend]/
      | - docker-compose.yml
      | - ...
    |-[nginx]/
      | - ...
    |-[gradle]/
      | - ...
    | - gradlew
    | - gradlew.bat
    | - Dockerfile.dev
    | - docker-compose.yml
    | - ...
```
> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

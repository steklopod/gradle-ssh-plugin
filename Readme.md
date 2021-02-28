## 🛡 [`SSH`](https://plugins.gradle.org/plugin/online.colaba.ssh) - gradle plugin for easy deploy by ftp 
![Backend CI](https://github.com/steklopod/gradle-ssh-plugin/workflows/Backend%20CI/badge.svg) [![Build Status](https://travis-ci.com/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.com/steklopod/gradle-ssh-plugin) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

### Quick start
1. You must have `id_rsa` private key (on your local machine: `{user.home}/.ssh/id_rsa` or in root of project) to use this plugin

2. In your `build.gradle.kts` file

```kotlin
plugins {
     id("online.colaba.ssh") version "1.3.25"
}

ssh {
    host = "hostexample.com"
    directory = "distribution"
}
```
> This tasks will copy local project **distribution** folder into remote **~/{project.name}/** and print it [will execute by ftp with ssh]

### 🎯 Run task:
```shell script
gradle ssh
```

### Customization:

#### [CLOUD mode] for deploying Spring Cloud microservices stack (no documentation).
> [DOCUMENTATION NEEDED issue for [CLOUD mode] microservices deployment](https://github.com/steklopod/gradle-ssh-plugin/issues/1)

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
### Available gradle tasks from `ssh` plugin:

Send by `ftp` with `ssh` (copy from local to remote server):
1. `ssh-backend` - copy **backend** distribution `*.jar`-file
2. `ssh-frontend` - copy **frontend** folder
3. `ssh-nginx` - copy **nginx** folder
4. `ssh-gradle` - copy **gradle** needed files
5. `ssh-docker` - copy **docker** files

All this tasks **includes** in 1 task:

* `publish` - all enabled  by default (**true**)

All this tasks **excluded** in 1 task:
* `ssh` task, where all disabled  by default (**false**) but can be included manually.

Other tasks:

* `compose` - docker compose up all docker-services(_gradle subprojects_) with recreate and rebuild
* `compose-nginx`, `compose-backend`, `compose-frontend` - docker compose up subproject with recreate and rebuild 
* `prune` - remove unused docker data
> [DOCUMENTATION NEEDED issue](https://github.com/steklopod/gradle-ssh-plugin/issues/1)

> Name of service for all tasks equals to ${project.name} 

___
### Preconfigured tasks for publishing/copy by ftp to remote server

By default you have preconfigured profiles tasks: 
* `ssh` - all disabled  by default (**false**)
* `publish` - all enabled  by default (**true**) [TODO documentation for `jars` property for this task]
> [DOCUMENTATION NEEDED issue for [CLOUD mode] microservices deployment](https://github.com/steklopod/gradle-ssh-plugin/issues/1)


You can customize these properties:
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

___


> [MONOLIT mode] Project's structure example for a backend `monolit` architecture
```shell script
 project
   |-[backend]
              | - [build/libs]/*.jar
              | - Dockerfile
              | - Dockerfile.dev
              | - docker-compose.yml
              | - docker-compose.dev.yml
              | - ...
   |-[frontend]
              | - docker-compose.yml
              | - ...
   |-[nginx]
              | - ...
   |-[gradle]
              | - ...
   |- gradlew
   |- gradlew.bat
   |- docker-compose.yml
   |- ...

```
> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`
> [DOCUMENTATION NEEDED issue for [CLOUD mode] microservices deployment](https://github.com/steklopod/gradle-ssh-plugin/issues/1)

___

##### Optional

With `ssh` plugin you have additional bonus task for executing a command line process on local PC [linux/windows]:
```kotlin
tasks{
      cmd { command = "echo ${project.name}" }
}
```
> [DOCUMENTATION NEEDED issue](https://github.com/steklopod/gradle-ssh-plugin/issues/1)

___
### TODO documentation for [CLOUD mode] microservices deployment (`ssh-jars` task)

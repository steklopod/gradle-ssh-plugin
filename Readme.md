## 🛡 [`SSH`](https://plugins.gradle.org/plugin/online.colaba.ssh) - gradle plugin for easy deploy by ftp 
![Backend CI](https://github.com/steklopod/gradle-ssh-plugin/workflows/Backend%20CI/badge.svg) [![Build Status](https://travis-ci.com/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.com/steklopod/gradle-ssh-plugin) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-ssh-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-ssh-plugin)

### Copy from local to remote server. 
#### From zero-config to full customization

* There will be as many tasks as gradle subprojects in your project.

### Quick start

In root project `build.gradle.kts` file:

```kotlin
plugins {
    id("online.colaba.ssh") version "1.4.34"
}

tasks {
    publish { 
        host = "my-ip-or-domain.com"
    }
}
```
That's all. 

This task will copy folders & files from local machine to remote host **~/${project.name}/...** folder 

### 🎯 Run task:
```shell script
gradle publish
```

### Customization:

1. Register new task in your `build.gradle.kts`:
```kotlin
register("customSshTask", Ssh::class) {
   host = "my-domain.com"
   user = "root"
   gradle = true
   frontend = false
   docker = true
   nginx = true
   directory = "distribution"
   run = "cd ${project.name} && echo \$PWD"
}
```
2. Run this task:
```shell script
gradle customSshTask
```

___
### Available gradle tasks from `ssh` plugin:

By default you have preconfigured tasks:
* `ssh` - all options are `disabled`  by default (**false**)
* `publish` - all options are `enabled` by default (**true**)
* `ssh-gradle` - copy **gradle** needed files to remote server in every subproject
* `ssh-docker` - copy **docker** files to remote server
* `ssh-jars` - copy **${subproject}/nuild/libs/___.jar** file to remote server  in every subproject

#### Example of tasks which will become available for your project:
1. `ssh-backend` - copy **backend** distribution `*.jar`-file to remote server
2. `ssh-frontend` - copy **frontend** folder to remote server
3. `ssh-nginx` - copy **nginx** folder to remote server
4. ...

___
Other tasks:

* `compose` - docker compose up all docker-services(_gradle subprojects_) with recreate and rebuild
* `compose-nginx`, `compose-backend`, `compose-frontend` - docker compose up subproject with recreate and rebuild 
* `prune` - remove unused docker data

> Name of service for all tasks equals to ${project.name} 
___


You can customize these properties:
```kotlin
ssh {
   host = "hostexample.com"
   directory = "copy_me_to_remote"
   nginx = true
}
```

___


> Project's structure example. There could be as many backends as you need.
```shell script
 project
   |-[backend]
              | - [src/main/java/build/libs]/*.jar
              | - Dockerfile
              | - Dockerfile.dev
              | - docker-compose.yml
              | - docker-compose.dev.yml
              | - ...
   |-[backend-2]
              | - [src/main/koltin/build/libs]/*.jar
              | - ...
   |-[backend-3]
              | - [src/main/scala/build/libs]/*.jar
              | - ...
   |-[frontend]
              | - docker-compose.yml
              | - ...
   |-[nginx]
              | - ...
   |-[postgres]
              | - [backups]
              | - ...
   |-[elastic]
              | - ...
   |-[static]
              | - ...
   |-[gradle]
              | - [wrapper]
   |- gradlew
   |- gradlew.bat
   |- docker-compose.yml
   |- ...

```
___

##### Bonus

With `ssh` plugin you have additional bonus task for executing a command line process [linux/windows]:
```kotlin
tasks{
      cmd { command = "echo ${project.name}" }
}
```
___
### Information:
For plugin usage you should have `id_rsa` key in root of project for `Continius Dilivery` or
on your local machine: `{user.home}/.ssh/id_rsa` for deployment from local machine. To make it run:

> on local machine:
```shell
ssh-keygen -t rsa -m PEM
chmod 400 ~/.ssh/id_rsa
ssh-copy-id -i ./id_rsa.pub root@my.server.ip
ssh-add .ssh/id_rsa
```

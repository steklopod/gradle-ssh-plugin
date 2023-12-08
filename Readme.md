## 🛡 [`SSH`](https://plugins.gradle.org/plugin/online.colaba.ssh) - gradle plugin for easy deployment
![Backend CI](https://github.com/steklopod/gradle-ssh-plugin/workflows/Backend%20CI/badge.svg) [![Build Status](https://travis-ci.com/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.com/steklopod/gradle-ssh-plugin) 

### Copy from local to remote server. 
Deliver your distribution to remoter server. 

* From **zero-config** to full customization.

### 🎯 Quick start

In root project `build.gradle.kts` file:

```kotlin
plugins {
    id("online.colaba.ssh") version "1.9.6"
}
group = "online.colaba"

```
That's all!

This task will copy folders & files from local machine to remote host **~/${project.name}/...** folder 

> You can set host, or it will be computed from `project.group` (example above)

```kotlin
tasks {
    scp { 
        host = "my-domain.com"
    }
}
```
#### Run task:
```shell script
gradle scp
```
___
### 🔮 Customization:

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

#### ☝️ `id_rsa` private key:
There must be **id_rsa** private key in root of your project.
* To generate it on local machine (_password will be requested_) and put it to remote server:
```shell
cd .ssh
ssh-keygen -m PEM -t rsa -b 2048
ssh-add id_rsa
ssh-copy-id -i id_rsa.pub root@my-server.com
```
___
### 🌀 Available gradle tasks from `ssh` plugin:

By default you have preconfigured tasks:
* `ssh` - all options are `disabled`  by default (**false**)
* `scp` - all options are `enabled` by default (**true**)
* `ssh-gradle` - copy **gradle** needed files to remote server in every subproject
* `ssh-docker` - copy **docker** files to remote server
* `ssh-jars` - copy **${subproject}/nuild/libs/___.jar** file to remote server  in every subproject
* and others: `ssh-ngix`, `ssh-briker`, `ssh-elasticsearch`, `ssh-docker`... 

Run `gradle tasks` to see the full list in groups `ssh`, `docker-main-${project.name}`.

#### Example of tasks, which will be available for your project:
* There will be as many tasks as gradle subprojects.

1. `ssh-backend` - copy **backend** distribution `*.jar`-file to remote server
2. `ssh-frontend` - copy **frontend** folder to remote server
3. `ssh-nginx` - copy **nginx** folder to remote server
4. ...
> Name of service for all tasks equals to ${project.name} 
___

### 🔮 Customization
You can customize these properties:
```kotlin
ssh {
   host = "hostexample.com"
   directory = "copy_me_to_remote"
   nginx = true
}
```
___


### 📋 Project's structure example
* There could be as many backends as you need.
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

##### 🔫  Bonus tasks:
With `ssh` plugin you have additional bonus **task** to help you with deploying applications with `docker`:
* `compose` - docker compose up all docker-services(_gradle subprojects_) with recreate and rebuild;
* `compose-nginx`, `compose-backend`, `compose-frontend`... - docker compose up subproject;
* `prune` - remove unused docker data;
* `cmd` - execute a command line process [linux/windows].

```kotlin
tasks{
      cmd { command = "echo ${project.name} 🧒" }
}
```

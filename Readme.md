# Gradle `ssh` plugin  [![Build Status](https://travis-ci.org/steklopod/gradle-ssh-plugin.svg?branch=master)](https://travis-ci.org/steklopod/gradle-ssh-plugin)
🛡️ Gradle `ssh` plugin for root multi-project

> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

### Structure must be
```shell script
 project
    |-backend/
      | - build/libs/*.jar
    |-frontend/
      | - dist/
```

### Quick start (`1 step only`)
1. In your `build.gradle.kts` file:
```kotlin
plugins {
     id("online.colaba.ssh") version "0.1.4"
}
```

### Execute by FTP 🎯
1. Variant 1
```shell script
./gradlew publish
```
2. Variant 2
```shell script
gradle ssh
```

### Available gradle tasks for `ssh` plugin`:

> Name of service for all tasks equals to ${project.name} 

* `publish` - send by ftp
* `ssh` - send by ftp


### Customization
1. Variant 1
```kotlin
        publish{
            host = "0.0.0.0"; user = "user"
            commands = "echo PUBLISH_COMMAND"
        }
```
2. Variant 2
```kotlin

        tasks {
            val ssh by registering(Publisher::class) {
            host = "0.0.0.0"; user = "user"
            commands = "echo SSH_COMMAND"
            }
        }
```

> Default `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

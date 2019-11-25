# Gradle `dockerMain` plugin  [![Build Status](https://travis-ci.org/steklopod/gradle-docker-main-plugin.svg?branch=master)](https://travis-ci.org/steklopod/gradle-docker-main-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-docker-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=steklopod_gradle-docker-plugin)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-docker-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=steklopod_gradle-docker-plugin)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-docker-plugin&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=steklopod_gradle-docker-plugin)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-docker-plugin&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-docker-plugin)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-docker-plugin&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-docker-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=steklopod_gradle-docker-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=steklopod_gradle-docker-plugin)

🛡️ Gradle `dockerMain` plugin for root multi-project now include in `ssh` plugin

### Requirement

* `backend`'s **jar** distribution path: `${project.rootDir}/backend/build/libs/*.jar`

* `frontend`'s **dist** folder path: `${project.rootDir}/frontend/dist`

You only need to have `docker-compose.yml` file in root of project

### Structure may be
```shell script
 root
    |-|backend/
    |  - build/libs/*.jar
    |  - build.gradle.kts
    |  - docker-compose.yml
    |  - docker-compose.dev.yml (optional)
    |
    |-|frontend/
    |  - |dist/
    |  - build.gradle.kts
    |  - docker-compose.yml
    |  - docker-compose.dev.yml (optional)
    |
    |-|nginx/
    |   - build.gradle.kts
    |   - docker-compose.yml
    |   - default.conf
    |   - domain.crt (optional)
    |   - domain.key (optional)
    |   - docker-compose.dev.yml (optional)
```


### Rerun/start 🎯
```shell script
./gradlew compose
```

### Available gradle tasks for `dockerMain` plugin`:

> Name of service for all tasks equals to ${project.name} 

* `removeAll` - remove all containers

* `compose` - docker compose up all docker-services with recreate and rebuild
* `composeNginx`, `composeBack`, `composeFront` - compose up with recreate and rebuild

* `prune` - remove unused data

* `recomposeAll`  - compose up after removing current docker-service

___
##### Optional

> `docker-compose.dev.yml`, `Dockerfile` & `Dockerfile.dev` files are optionals.

Optional tasks: 

* `composeDev` - compose up all docker-services from `docker-compose.dev.yml` file with recreate and rebuild. 
Depends on `backend:assemble`;
* `recomposeAllDev` - compose up after removing current docker-service from `docker-compose.dev.yml` file. 

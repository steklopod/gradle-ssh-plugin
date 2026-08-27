package online.colaba

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType

class SshPlugin : Plugin<Project> { override fun apply(project: Project): Unit = project.run {
description = "🚐 Deploy your multi-module gradle project distribution by ssh. + 🐳 Docker-compose bonus tasks "


registerSshTask(); registerScpTask(); registerJarsTask(); registerFrontTask(); registerPostgresTask()
ssh { }
scp { }
sshJars { }
sshFront { }
sshPostgres { }

registerCmdTask(); registerDckrTask(); registerDockerComposeTask(); registerDockerComposeUpTask(); registerDockerRolloutTask();
cmd { }
dckr { }
compose { }
composeUp { }
rollout { }

val (backendJARs, _) = subprojects
    .filter { !it.name.endsWith("lib") && !it.name.contains("postgres") && !it.name.contains("front") }
    .partition { it.localExists("src/main") || it.localExists("build/libs") }


tasks {
    backendJARs.map{it.name}.forEach { register<Ssh>("ssh-${it}") { directory = jarLibFolder(it); description = "🦉 Copy backend [${jarLibFolder(it)}] jar to remote server" } }

    // 🚫 `ssh-<папка>` через `directory` ставит на деплой не файл, а КАТАЛОГ, а copyWithOverride
    // сперва делает rm -rf удалённого каталога и только потом льёт локальный целиком - вместе с
    // node_modules и всем, что валяется в рабочем дереве, унося при этом compose.yml и .dockerignore
    // сервера. После починки фильтра выше (он читал `name` корневого проекта вместо `it.name`, то
    // есть был константным) здесь и оставались ровно те два подпроекта, которые фильтр обязан был
    // убрать: frontend и postgres. У обоих свой безопасный путь - sshFront кладёт .output.tar.xz
    // плюс compose/Dockerfile, а у postgres отдельная ветка в Ssh.kt, берегущая backups.
//    wholeFolder.map{it.name}
//        .filter{ !it.contains("static") && !it.contains("monitor") && it != BROKER && it != NGINX && it != ELASTIC }
//        .forEach { register<Ssh>("ssh-$it") { directory = it; description = "🦖 Copy WHOLE FOLDER [$it] to remote server" } }

    register<Ssh>("ssh-docker"){ docker = true; allProjects = true; description = "🐳 Copy [docker] needed files to remote server including subprojects" }
    register<Ssh>("ssh-gradle"){ gradle = true; allProjects = true; description = "🐘 Copy [gradle] needed files to remote server including subprojects" }
    register<Ssh>("ssh-static-force"){ staticOverride = true; finalizedBy(compose); description = "🌄 Force copy [static] with override to remote server"}
    register<Ssh>("ssh-$ELASTIC"){ elastic = true; description = "🔎 Deploy by scp whole [$ELASTIC] folder"  }
    register<Ssh>("ssh-$BROKER"){ broker = true; description = "🔎 Deploy by scp whole [$BROKER] folder"  }
    register<Ssh>("ssh-$VAULT"){ vault = true; description = "🔐 Deploy by scp [$VAULT] folder (config/agent/policies)"  }
    register<Ssh>("ssh-$NGINX"){ nginx = true; description = "🔎 Deploy by scp whole [$NGINX] folder"  }
    register<Ssh>("ssh-monitoring"){ monitoring = true; description = "🔎 Deploy by scp whole [MONITORING] folder"  }

    register<Ssh>("clear-frontend"){ frontendClearOnly = true;  group = "help"; description = "🗑 Remove local [node_modules] & [.nuxt , .output], pacakage-lock.json" }
    register<Ssh>("ssh-frontend-whole"){ frontend = true; frontendWhole = true; description = "📱 Deploy by scp WHOLE [frontend] folder" }

    // DOCKER COMPOSE
    subprojects.filter { !name.endsWith("lib") && !name.contains("static") }
               .forEach { register<DockerComposeUp>("compose-${it.name}"){ service = it.name; description = "🐳 Docker compose up for [${it.name}] container" } }

    // ZERO-DOWNTIME ROLLOUT (backend JVM-сервисы: healthcheck + eureka discovery). Требует
    // docker-rollout CLI-плагин на хосте и отсутствие container_name у сервиса (scale=2).
    backendJARs.map { it.name }
               .forEach { svc -> register<DockerRollout>("rollout-$svc"){
                   service = svc
                   // graceful shutdown (shutdown: graceful) auto-deregisters from eureka on SIGTERM and
                   // drains in-flight; waitAfterHealthy gives gateway-LB / nginx time to converge to the
                   // new instance before the old dies. Together -> zero lost requests (proven under load).
                   description = "🐳 Zero-downtime rollout for [$svc]"
               } }

    val ps by registering (Dckr::class) { exec = "ps"; description = "🐳 Print all containers to console output" }

    val down by registering (DockerCompose::class) { exec = "down -fvs";   description = "🐳🙈 stop containers and removes containers, networks, volumes, and images created by up"}
    val prune by registering (Dckr::class) { exec = "system prune -fa"; description = "🐳🗑 Remove unused docker data"; finalizedBy(ps) }
    val networkPrune by registering (Dckr::class) { exec ="network prune -f"; description = "🐳🗑 Remove unused docker networks" }

    val rmPostgresVolume by registering (Dckr::class) { exec = "volume rm -f ${project.name}_postgres-data"; description = "🐳🗑 Remove volume postgres"}
    val rmElasticVolume by registering (Dckr::class) { exec = "volume rm -f ${project.name}_elastic-data"; description = "🐳🗑 Remove volume elastic"}
    val volumesRm by registering (Dckr::class) { dependsOn(rmElasticVolume); finalizedBy(rmPostgresVolume); exec = "🐳🗑 volume rm -f ${project.name}_backups"; description = "Remove volume backups"}
    val volumePrune by registering (Dckr::class) { dependsOn(down); finalizedBy(volumesRm); description = "🗑🙈 Docker down & Volume prune"}
    val rmStaticVlm by registering (Dckr::class) { dependsOn(volumePrune); exec ="🐳🗑 volume rm -f ${project.name}_static"; finalizedBy(networkPrune)}

    val stopAll by registering (DockerCompose::class) { exec ="down -v"; description = "🐳🙈 Stop all docker containers" }
    register<DockerCompose>("pruneAll") {
        dependsOn(stopAll)
        exec = "rm -f"
        description = "🐳🐳🗑🗑🙈🙈 Docker remove all containers & volumes & networks & images"
        finalizedBy(prune)
    }

    // COLABA banner, printed once per build: registered as a finalizer (deduped across the run), so
    // several ssh tasks in one invocation share a single banner instead of one each.
    val colaba by registering {
        group = sshGroup
        description = "🩸 COLABA deploy banner (prints once after ssh tasks)"
        doLast {
            println("\n🩸🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🩸🩸🩸")
            println("🩸🩸🔫🔫🔫 C O L A B A 🔫🔫🔫🩸🩸")
            println("🩸🩸🩸🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🔫🩸\n")
        }
    }
    withType<Ssh>().configureEach { finalizedBy(colaba) }

} } }

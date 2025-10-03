package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class DockerComposeUp : Cmd() {
    init {
        group = "docker-main-${project.name}"
        description = "🐳 Docker compose UP task"
    }

    @get:Input @Optional var composeFile: String? = null
    @get:Input @Optional var service    : String? = null

    @get:Input var exec: String = "up "

    @get:Input var noDeps       : Boolean = true
    @get:Input var recreate     : Boolean = true
    @get:Input var forceRecreate: Boolean = true

    @get:Input var errorTailCount : Int = 25

    @TaskAction
    override fun exec() {
        var fullCommand = exec
        var rebuildFlag = "--build"

        if (noDeps) rebuildFlag += " --no-deps"
        if (forceRecreate) rebuildFlag += " --force-recreate"

        composeFile?.run { fullCommand = "-f $this up" }

        if (recreate) fullCommand += rebuildFlag

        service?.let { fullCommand += " $it" }

        val runCommand = "docker compose $fullCommand --detach".trim()


        val startTime = System.currentTimeMillis()
        println("┌─────────────────────────────────────────┐")
        println("│  🐳 Docker Compose Deployment Started   │")
        println("└─────────────────────────────────────────┘")
        println("📋 Command: $runCommand")
        println("🎯 Service: ${service ?: "All services"}")
        println("📁 Compose file: ${composeFile ?: "docker-compose.yml"}")
        if (System.getProperty("COMPOSE_BAKE") == "true") println("⚙️ Bake: ✅ Enabled")
        println()

        try {
            super.command = runCommand
            super.exec()

            val duration = System.currentTimeMillis() - startTime
            println()
            println("┌─────────────────────────────────────────┐")
            println("│   ✅ Deployment Completed Successfully  │")
            println("└─────────────────────────────────────────┘")
            println("⏱️ Duration: ${duration/1000}s")
            println("🚀 Services should be starting up...")
        } catch (e: Exception) {
            println()
            println("┌─────────────────────────────────────────┐")
            println("│      ❌ Deployment Failed!              │")
            println("└─────────────────────────────────────────┘")
            println("💥 Error: ${e.message}")

            // Пытаемся показать логи контейнера если он существует
            service?.let { serviceName ->
                println()
                println("🔍 Attempting to fetch logs for service '$serviceName'...")
                try {
                    val logsCommand = "docker compose logs --tail=$errorTailCount $serviceName"
                    val logsProcess = ProcessBuilder(logsCommand.split(" "))
                        .directory(project.projectDir)
                        .inheritIO()
                        .start()
                    logsProcess.waitFor()
                } catch (logsException: Exception) {
                    println("⚠️ Could not fetch logs: ${logsException.message}")
                }

                println()
                println("🔍 Container status:")
                try {
                    val psCommand = "docker compose ps $serviceName"
                    val psProcess = ProcessBuilder(psCommand.split(" ")).directory(project.projectDir).inheritIO().start()
                    psProcess.waitFor()
                } catch (psException: Exception) {
                    println("⚠️ Could not get container status: ${psException.message}")
                }
            }
            throw e
        }
    }
}

fun Project.registerDockerComposeUpTask() = tasks.register<DockerComposeUp>("composeUp")

val Project.composeUp: TaskProvider<DockerComposeUp>
    get() = tasks.named<DockerComposeUp>("composeUp")

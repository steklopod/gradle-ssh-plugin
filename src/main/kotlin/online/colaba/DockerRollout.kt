package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Wraps `docker rollout`; side effects on the local Docker daemon, no cacheable output.")
open class DockerRollout : Cmd() {
    init {
        group = "docker-main-${project.name}"
        description = "🐳 Zero-downtime rollout: scale up new instance, await healthcheck, drop old"
    }

    @get:Input @Optional var composeFile: String? = null
    @get:Input @Optional var service    : String? = null

    // Timeout for the NEW container to become healthy. JVM services start in ~60-90s,
    // so the default is higher than rollout's own 60s. The old instance keeps serving meanwhile.
    @get:Input var timeout: Int = 130

    @TaskAction
    override fun exec() {
        val svc = service ?: project.name
        val fileFlag = composeFile?.let { "--file $it " } ?: ""
        // docker rollout: scales the service to 2, waits for the new container's healthcheck, then stops the old one.
        // Requires the docker-rollout CLI plugin on the host and NO container_name on the service (scale=2 would clash).
        val runCommand = "docker rollout ${fileFlag}--timeout $timeout $svc".trim()

        println("┌─────────────────────────────────────────┐")
        println("│  🐳 Zero-downtime Rollout Started        │")
        println("└─────────────────────────────────────────┘")
        println("🎯 Service: $svc")
        println("📋 Command: $runCommand")
        println()

        super.command = runCommand
        super.exec()
    }
}

fun Project.registerDockerRolloutTask() = tasks.register<DockerRollout>("rollout")

val Project.rollout: TaskProvider<DockerRollout>
    get() = tasks.named<DockerRollout>("rollout")

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

    // Drain: keep the OLD instance alive N seconds after the new one is healthy, so discovery
    // (nginx DNS / eureka registry) converges to the new IP before the old dies. Closes the
    // "stale cached address" window -> zero lost requests (proven under load). See nginx/CLAUDE.md.
    @get:Input var waitAfterHealthy: Int = 8

    // Command run inside the OLD container right before stop (e.g. eureka deregister / drain).
    // Backend marks itself DOWN in eureka so gateway-LB stops routing before the old instance dies.
    @get:Input @Optional var preStopHook: String? = null

    @TaskAction
    override fun exec() {
        val svc = service ?: project.name
        val fileFlag = composeFile?.let { "--file $it " } ?: ""
        val drainFlag = if (waitAfterHealthy > 0) "--wait-after-healthy $waitAfterHealthy " else ""
        val hookFlag  = preStopHook?.let { "--pre-stop-hook \"$it\" " } ?: ""
        // docker rollout: scales the service to 2, awaits the new container's healthcheck, drains,
        // then stops the old. Requires docker-rollout CLI on host and NO container_name (scale=2 clashes).
        val runCommand = "docker rollout ${fileFlag}${drainFlag}${hookFlag}--timeout $timeout $svc".trim()

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

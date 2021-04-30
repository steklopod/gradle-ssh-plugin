package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class DockerCompose : Cmd() {
    companion object {
        fun dockerMainGroupName(projectName: String) = "docker-main-$projectName"
    }
    init {
        group = dockerMainGroupName(project.name)
        description = "Docker-compose task"
    }
    @get:Input var exec : String = "up"

    @TaskAction
    override fun exec() {
        super.command = "docker-compose $exec"
        super.exec()
    }
}

fun Project.registerDockerComposeTask() = tasks.register<DockerCompose>("compose")

val Project.compose: TaskProvider<DockerCompose>
    get() = tasks.named<DockerCompose>("compose")

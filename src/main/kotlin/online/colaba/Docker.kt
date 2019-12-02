package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class Docker : Executor() {
    init {
        group = "$dockerPrefix-${project.name}"
        description = "Docker task for customization (by default only print current docker-services)"
    }

    @get:Input
    var exec = "ps"

    @TaskAction
    override fun exec() {
        super.command = "$dockerPrefix $exec"
        super.exec()
    }
}

fun Project.registerDockerTask() = tasks.register<Docker>("docker")

val Project.docker: TaskProvider<Docker>
    get() = tasks.named<Docker>("docker")

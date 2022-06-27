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
        description = "🐳 Docker-compose UP task"
    }

    @get:Input @Optional var composeFile: String? = null
    @get:Input @Optional var service    : String? = null

    @get:Input var exec    : String = "up "
    @get:Input var recreate: Boolean = true
    @get:Input var noDeps  : Boolean = true
    @get:Input var forceRecreate  : Boolean = true

    @TaskAction
    override fun exec() {
        var fullCommand = exec

        var rebuildFlag = "--build"

        if (noDeps) rebuildFlag += " --no-deps"
        if (forceRecreate) rebuildFlag += " --force-recreate"

        composeFile?.run { fullCommand = "-f $this up" }

        if (recreate) fullCommand += rebuildFlag

        service?.let { fullCommand += " $it" }

        val runCommand = "docker-compose $fullCommand --detach".trim()
        println("🐳 $runCommand")

        super.command = runCommand
        super.exec()
    }
}

fun Project.registerDockerComposeUpTask() = tasks.register<DockerComposeUp>("composeUp")

val Project.composeUp: TaskProvider<DockerComposeUp>
    get() = tasks.named<DockerComposeUp>("composeUp")

package online.colaba

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class Cmd : DefaultTask() {

    init {
        group = "help"
        description = "🐙 Execute a command line process on local PC [linux/windows]"
    }

    @get:Input var command = "echo ${project.name}"

    @TaskAction
    open fun exec() {
        val commandList = cmdPrefix + command.splitBySpace()
        println("🐙 Executing command: $commandList")

        val process = ProcessBuilder(commandList)
            .directory(project.projectDir)
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Command failed with exit code: $exitCode")
        }
    }

    private fun String.splitBySpace(): List<String>  = replace("  ", " ").split(" ")
}

fun Project.registerCmdTask() = tasks.register<Cmd>("cmd")
val Project.cmd: TaskProvider<Cmd>
    get() = tasks.named<Cmd>("cmd")

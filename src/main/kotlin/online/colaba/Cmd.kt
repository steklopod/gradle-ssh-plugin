package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class Cmd : Exec() {
    init {
        group = "execute"
        description = "Execute a command line process on local PC [linux/windows]"
    }

    @get:Input
    var command = "echo ${project.name}"

    @TaskAction
    override fun exec() {
        try {
            commandLine = windowsPrefix + command.splitBySpace()
            println("> Executing command: $commandLine\n")
            super.exec()
        } catch (e: Exception) {
            e.shortStackTraceWithPrint(project.name)
        }
    }


    fun dockerForEachSubproject(project: Project, dockerCommand: String, vararg ignoringServices: String) {
        project.subprojects.forEach {
            val name = it.name
            if (!ignoringServices.toSet().contains(name)) {
                command = "docker $dockerCommand $name"
            }
        }
    }
}


fun Project.registerCmdTask() = tasks.register<Cmd>("cmd")

val Project.cmd: TaskProvider<Cmd>
    get() = tasks.named<Cmd>("cmd")

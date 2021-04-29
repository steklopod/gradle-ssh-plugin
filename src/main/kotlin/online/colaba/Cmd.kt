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

    @get:Input var command = "echo ${project.name}"

    @TaskAction override fun exec() {
        commandLine = windowsPrefix + command.splitBySpace()
        println("> Executing command: $commandLine\n")
        super.exec()
    }

    fun composeForEachSubproject(project: Project, dockerCommand: String, vararg ignoringServices: String) {
        val services = project.subprojects.map { it.name }.filter { !it.contains("static") && !ignoringServices.toSet().contains(it) }
        services.forEach {
            command = "docker-compose $dockerCommand $it"
            exec()
        }
    }

    private fun String.splitBySpace(): List<String>  = replace("  ", " ").split(" ")
}

fun Project.registerCmdTask() = tasks.register<Cmd>("cmd")
val Project.cmd: TaskProvider<Cmd>
    get() = tasks.named<Cmd>("cmd")

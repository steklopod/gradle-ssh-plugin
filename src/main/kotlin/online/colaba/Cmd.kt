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
        group = "help"
        description = "🐙 Execute a command line process on local PC [linux/windows]"
    }

    @get:Input var command = "echo ${project.name}"

    @TaskAction
    override fun exec() {
        commandLine = cmdPrefix + command.splitBySpace()
        println("🐙 Executing command: $commandLine")
        super.exec()
    }

    fun composeForEachSubproject(project: Project, dockerCommand: String, vararg ignoringServices: String) {
        subprojectsNames(project, ignoringServices).forEach {
            command = "docker-compose $dockerCommand $it"
            exec()
        }
    }

    private fun subprojectsNames(project: Project, ignoringServices: Array<out String>): List<String> = project.subprojects.map { it.name }
        .filter { !it.contains("static") &&  !it.contains("-lib") && !ignoringServices.toSet().contains(it) }

    private fun String.splitBySpace(): List<String>  = replace("  ", " ").split(" ")
}

fun Project.registerCmdTask() = tasks.register<Cmd>("cmd")
val Project.cmd: TaskProvider<Cmd>
    get() = tasks.named<Cmd>("cmd")

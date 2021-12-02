package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class Dckr : Cmd() {
    init {
        group = "docker-main-${project.name}"
        description = "🐋 Docker task"
    }
    @get:Input @Optional var exec : String? = null

    @TaskAction
    override fun exec() { exec?.run {
        super.command = "docker $this"
        super.exec()
    } }

    fun rmVolumes(vararg volumes: String) {
        volumes.toSet().ifEmpty { listOf("backups", "elastic-data", "postgres-data") }.forEach {
            exec = "volume rm -f ${project.name}_$it"
            exec()
        }
    }
}

fun Project.registerDckrTask() = tasks.register<Dckr>("dckr")

val Project.dckr: TaskProvider<Dckr>
    get() = tasks.named<Dckr>("dckr")

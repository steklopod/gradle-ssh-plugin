package online.colaba

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register


open class OpenApiSchema : Cmd() {
    init {
        group = sshGroup
        description = "Generating [./frontend/types/schema.ts] from OPEN API docs.yaml"
    }

    @get:Input var fromFolder   : String = "/backend/src/test/resources"
    @get:Input var fromFilename : String = "docs.yaml"

    @get:Input var toFolder   : String = "/frontend/types"
    @get:Input var toFilename : String = "schema"

    @TaskAction
    fun run() {
        println("🔜 OPEN API: you should have  🧿$fromFolder/$fromFilename🧿 and [frontend] folder in root")
        val runCommand = "npx openapi-typescript ${project.rootDir}$fromFolder/$fromFilename --output ${project.rootDir}$toFolder/$toFilename.ts"
            .trim().normalizeForWindows()
        super.command = runCommand
        super.exec()
    }
}
fun Project.registerSchemaTask() = tasks.register<OpenApiSchema>("schema")

val Project.schema: TaskProvider<OpenApiSchema>
    get() = tasks.named<OpenApiSchema>("schema")

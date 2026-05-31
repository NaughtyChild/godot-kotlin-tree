package com.tomwyr

import com.tomwyr.command.CommandPaths
import com.tomwyr.command.GenerateTreeCommand
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class GodotKotlinTree : Plugin<Project> {
    override fun apply(project: Project) {
        val generateBindings = registerTask(project)
        wireKotlinTaskDependencies(project, generateBindings)
        addSourceSet(project)
    }

    private fun registerTask(project: Project): TaskProvider<*> {
        val input = project.extensions.create("godotNodeTree", GodotKotlinTreeInput::class.java)

        return project.tasks.register("generateGodotBindings") { task ->
            task.group = "godot kotlin tree"
            task.description =
                "Generates per-script `*Bindings` and `*Scene` Kotlin files from .tscn + .kt sources."

            task.inputs.files(
                project.fileTree(project.projectDir) {
                    it.include("**/*.tscn")
                    it.include("**/*.kt")
                    it.exclude("build/**")
                    it.exclude(".gradle/**")
                    it.exclude(".godot/**")
                }
            ).withPropertyName("godotSources")

            task.inputs.property("packageName", input.packageName ?: "")
            task.inputs.property("projectPath", input.projectPath ?: "")
            task.inputs.property("validateProjectPath", input.validateProjectPath)

            task.outputs.dir(
                project.layout.buildDirectory.dir("generated/godotBindings/kotlin")
            ).withPropertyName("generatedBindings")

            task.doLast {
                GenerateTreeCommand.from(project, input).run()
            }
        }
    }

    /**
     * `whenTaskAdded` does not fire for tasks that already exist when this plugin is applied
     * (e.g. `compileKotlin` registered by godot-kotlin-jvm). Wire dependencies in
     * `afterEvaluate` so clean builds always run binding generation before compilation/KSP.
     */
    private fun wireKotlinTaskDependencies(project: Project, generateBindings: TaskProvider<*>) {
        project.afterEvaluate {
            listOf("compileKotlin", "kspKotlin").forEach { taskName ->
                project.tasks.findByName(taskName)?.dependsOn(generateBindings)
            }
        }
    }

    private fun addSourceSet(project: Project) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val pluginExt = project.extensions.findByType(KotlinJvmProjectExtension::class.java)
            pluginExt?.sourceSets?.getByName("main")?.kotlin?.srcDir(
                project.layout.buildDirectory.dir("generated/godotBindings/kotlin")
            )
        }
    }
}

open class GodotKotlinTreeInput(
    var projectPath: String? = null,
    var validateProjectPath: Boolean = true,
    var packageName: String? = null,
)

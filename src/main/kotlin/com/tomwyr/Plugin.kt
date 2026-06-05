package com.tomwyr

import com.tomwyr.command.CommandPaths
import com.tomwyr.command.GenerateResCommand
import com.tomwyr.command.GenerateTreeCommand
import com.tomwyr.common.ResConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class GodotKotlinTree : Plugin<Project> {
    override fun apply(project: Project) {
        val input = project.extensions.create("godotNodeTree", GodotKotlinTreeInput::class.java)
        val generateRes = registerResTask(project, input)
        val generateBindings = registerBindingsTask(project, input, generateRes)
        wireKotlinTaskDependencies(project, generateBindings)
        addSourceSet(project)
    }

    private fun registerBindingsTask(
        project: Project,
        input: GodotKotlinTreeInput,
        generateRes: TaskProvider<*>,
    ): TaskProvider<*> {
        val bindings = project.tasks.register("generateGodotBindings") { task ->
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
        project.afterEvaluate {
            if (input.resExtensions.isNotEmpty()) {
                bindings.configure { task -> task.dependsOn(generateRes) }
            }
        }
        return bindings
    }

    private fun registerResTask(project: Project, input: GodotKotlinTreeInput): TaskProvider<*> {
        return project.tasks.register("generateGodotRes") { task ->
            task.group = "godot kotlin tree"
            task.description =
                "Generates Res.kt with res:// path constants for configured resource extensions."

            task.inputs.files(
                project.provider {
                    val extensions = input.resExtensions
                    val excludeDirs = ResConfig.resolveExcludeDirs(input.resExcludeDirs)
                    project.fileTree(project.projectDir) { ft ->
                        if (extensions.isEmpty()) {
                            ft.exclude("**/*")
                        } else {
                            extensions.forEach { ext ->
                                ft.include("**/*.${ext.lowercase().trimStart('.')}")
                            }
                        }
                        excludeDirs.forEach { dir ->
                            ft.exclude("**/$dir/**")
                            ft.exclude("$dir/**")
                        }
                        ft.exclude("**/*.import")
                        ft.exclude("**/*.uid")
                    }
                }
            ).withPropertyName("godotResources")

            task.inputs.property("packageName", input.packageName ?: "")
            task.inputs.property("projectPath", input.projectPath ?: "")
            task.inputs.property("validateProjectPath", input.validateProjectPath)
            task.inputs.property("resExtensions", project.provider { input.resExtensions.toList() })
            task.inputs.property("resExcludeDirs", project.provider {
                if (input.resExcludeDirs === GodotKotlinTreeInput.EXCLUDE_DIRS_DEFAULT_SENTINEL) {
                    "<DEFAULT>"
                } else {
                    input.resExcludeDirs.toString()
                }
            })

            task.outputs.file(
                project.layout.buildDirectory.file(
                    project.provider {
                        CommandPaths.resOutputRelativePath(input.packageName)
                    }
                )
            ).withPropertyName("generatedRes")

            task.doLast {
                GenerateResCommand.from(project, input).run()
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
    var resExtensions: List<String> = emptyList(),
    var resExcludeDirs: List<String> = EXCLUDE_DIRS_DEFAULT_SENTINEL,
) {
    companion object {
        internal val EXCLUDE_DIRS_DEFAULT_SENTINEL: List<String> =
            listOf("\u0000__godot-kotlin-tree:exclude-dirs-default__\u0000")
    }
}

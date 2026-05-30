package com.tomwyr.command

import com.tomwyr.GodotKotlinTreeInput
import com.tomwyr.parser.GdjScanner
import com.tomwyr.parser.MountTreeBuilder
import com.tomwyr.parser.ProjectScanner
import org.gradle.api.Project
import java.nio.file.Paths

class GenerateTreeCommand(
    val projectPath: String,
    val validateProjectPath: Boolean,
    val outputDir: String,
    val packageName: String?,
) {
    fun run() {
        val projectRoot = Paths.get(projectPath)

        val gdjIndex = GdjScanner.scan(projectRoot)

        val tscnFiles = ProjectScanner.findTscnFiles(projectRoot, validateProjectPath)

        val buildResult = MountTreeBuilder.build(
            tscnFiles = tscnFiles,
            projectRoot = projectRoot,
            gdjIndex = gdjIndex,
        )

        for (warning in buildResult.warnings) {
            System.err.println(warning)
        }

        val files = BindingsRenderer(packageName).render(buildResult)

        MultiFileWriter().write(outputDir, files)
    }

    companion object Factory {
        fun from(project: Project, input: GodotKotlinTreeInput): GenerateTreeCommand {
            val rootPath = project.projectDir.absolutePath
            val projectPath = resolveProjectPath(rootPath, input.projectPath, input.validateProjectPath)
            val outputDir = Paths.get(
                rootPath, "build", "generated", "godotBindings", "kotlin"
            ).toString()

            return GenerateTreeCommand(
                projectPath = projectPath,
                validateProjectPath = input.validateProjectPath,
                outputDir = outputDir,
                packageName = input.packageName,
            )
        }

        private fun resolveProjectPath(rootPath: String, relativePath: String?, validate: Boolean): String {
            val base = Paths.get(rootPath, relativePath ?: "")
            return if (!validate && relativePath == null) {
                base.toString()
            } else {
                val projectFile = base.resolve("project.godot")
                if (projectFile.toFile().exists()) base.toString() else base.toString()
            }
        }
    }
}

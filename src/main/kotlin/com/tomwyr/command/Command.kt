package com.tomwyr.command

import com.tomwyr.GodotKotlinTreeInput
import com.tomwyr.parser.KtSourceLoader
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

        val ktLoader = KtSourceLoader(projectRoot)

        val tscnFiles = ProjectScanner.findTscnFiles(projectRoot, validateProjectPath)

        val buildResult = MountTreeBuilder.build(
            tscnFiles = tscnFiles,
            projectRoot = projectRoot,
            ktLoader = ktLoader,
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
            val projectPath = CommandPaths.resolveProjectPath(
                rootPath = rootPath,
                relativePath = input.projectPath,
                validate = input.validateProjectPath,
            )

            return GenerateTreeCommand(
                projectPath = projectPath,
                validateProjectPath = input.validateProjectPath,
                outputDir = CommandPaths.outputDir(project),
                packageName = input.packageName,
            )
        }
    }
}

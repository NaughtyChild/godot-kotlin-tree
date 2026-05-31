package com.tomwyr.command

import com.tomwyr.GodotKotlinTreeInput
import com.tomwyr.common.ResConfig
import com.tomwyr.common.ResConflictValidator
import com.tomwyr.common.ResourceEntry
import com.tomwyr.parser.ResScanner
import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.Paths

class GenerateResCommand(
    val projectPath: String,
    val validateProjectPath: Boolean,
    val outputFile: Path,
    val packageName: String?,
    val resExtensions: List<String>,
    val resExcludeDirs: List<String>,
) {
    fun run() {
        val entries = if (resExtensions.isEmpty()) {
            emptyList()
        } else {
            ResScanner.scan(
                projectPath = Paths.get(projectPath),
                validate = validateProjectPath,
                extensions = resExtensions,
                excludeDirs = ResConfig.resolveExcludeDirs(resExcludeDirs),
            )
        }

        validate(entries)

        val content = ResRenderer(packageName).render(entries)
        SingleFileWriter().write(outputFile, content)
    }

    private fun validate(entries: List<ResourceEntry>) {
        ResConflictValidator.validate(entries)
    }

    companion object Factory {
        fun from(project: Project, input: GodotKotlinTreeInput): GenerateResCommand {
            val rootPath = project.projectDir.absolutePath
            val projectPath = CommandPaths.resolveProjectPath(
                rootPath = rootPath,
                relativePath = input.projectPath,
                validate = input.validateProjectPath,
            )
            return GenerateResCommand(
                projectPath = projectPath,
                validateProjectPath = input.validateProjectPath,
                outputFile = CommandPaths.resOutputFile(project, input.packageName),
                packageName = input.packageName,
                resExtensions = input.resExtensions,
                resExcludeDirs = input.resExcludeDirs,
            )
        }
    }
}

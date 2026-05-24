package com.tomwyr.parser

import com.tomwyr.common.GeneratorError
import com.tomwyr.common.InvalidGodotProject
import com.tomwyr.common.ScanningScenesFailed
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object ProjectScanner {
    fun findTscnFiles(projectPath: Path, validate: Boolean): List<Path> {
        val rootDir = resolveRootDir(projectPath, validate)
        return try {
            Files.walk(rootDir).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter { it.fileName.toString().endsWith(".tscn", ignoreCase = true) }
                    .sorted()
                    .toList()
            }
        } catch (e: IOException) {
            throw GeneratorError(ScanningScenesFailed(rootDir.toString()))
        }
    }

    private fun resolveRootDir(projectPath: Path, validate: Boolean): Path {
        if (projectPath.isDirectory()) {
            if (validate) {
                val projectFile = projectPath.resolve("project.godot")
                if (!projectFile.exists()) {
                    throw GeneratorError(InvalidGodotProject(projectPath.toString()))
                }
            }
            return projectPath
        }

        if (validate && !projectPath.exists()) {
            throw GeneratorError(InvalidGodotProject(projectPath.toString()))
        }

        return projectPath.parent
            ?: throw GeneratorError(InvalidGodotProject(projectPath.toString()))
    }
}

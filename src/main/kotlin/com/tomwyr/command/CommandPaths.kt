package com.tomwyr.command

import org.gradle.api.Project
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Centralised path resolution shared by godot-kotlin-tree commands so that
 * project-root and output-directory rules don't drift between callers.
 */
object CommandPaths {
    private const val OUTPUT_SUBPATH = "build/generated/godotBindings/kotlin"

    fun resolveProjectPath(rootPath: String, relativePath: String?, validate: Boolean): String {
        val base = Paths.get(rootPath, relativePath ?: "")
        return if (!validate && relativePath == null) {
            base.toString()
        } else {
            base.toString()
        }
    }

    fun outputDir(project: Project): String =
        Paths.get(project.projectDir.absolutePath, *OUTPUT_SUBPATH.split('/').toTypedArray()).toString()

    fun outputDir(rootPath: String): Path =
        Paths.get(rootPath, *OUTPUT_SUBPATH.split('/').toTypedArray())

    fun resOutputFile(project: Project, packageName: String?): Path {
        val pkgPath = packageName?.replace('.', '/') ?: ""
        val base = outputDir(project.projectDir.absolutePath)
        return if (pkgPath.isEmpty()) base.resolve("Res.kt") else base.resolve("$pkgPath/Res.kt")
    }

    fun resOutputRelativePath(packageName: String?): String {
        val pkgPath = packageName?.replace('.', '/') ?: ""
        return if (pkgPath.isEmpty()) {
            "$OUTPUT_SUBPATH/Res.kt"
        } else {
            "$OUTPUT_SUBPATH/$pkgPath/Res.kt"
        }
    }
}

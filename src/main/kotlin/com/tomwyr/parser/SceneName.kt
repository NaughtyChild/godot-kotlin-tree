package com.tomwyr.parser

import java.nio.file.Path

object SceneName {
    fun fromFile(file: Path): String = toPascalCase(stripExtension(file.fileName.toString()))

    fun fromResourcePath(resourcePath: String): String {
        val withoutScheme = resourcePath.removePrefix("res://")
        val fileName = withoutScheme.substringAfterLast('/')
        return toPascalCase(stripExtension(fileName))
    }

    private fun stripExtension(fileName: String): String =
        if (fileName.endsWith(".tscn", ignoreCase = true)) fileName.dropLast(5) else fileName

    private fun toPascalCase(name: String): String =
        name.split('_')
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}

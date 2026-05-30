package com.tomwyr.parser

import com.tomwyr.common.GdjInfo
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object GdjScanner {
    fun scan(projectRoot: Path): Map<String, GdjInfo> {
        val result = mutableMapOf<String, GdjInfo>()
        try {
            Files.walk(projectRoot).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter { it.fileName.toString().endsWith(".gdj", ignoreCase = true) }
                    .forEach { gdjFile ->
                        try {
                            val content = Files.readString(gdjFile)
                            val (key, info) = parseGdj(content) ?: return@forEach
                            if (result.containsKey(key)) {
                                System.err.println(
                                    "[godot-kotlin-tree] Warning: duplicate .gdj for relativeSourcePath=$key"
                                )
                            }
                            result[key] = info
                        } catch (e: IOException) {
                            System.err.println(
                                "[godot-kotlin-tree] Warning: failed to read .gdj file: $gdjFile"
                            )
                        }
                    }
            }
        } catch (e: IOException) {
            System.err.println(
                "[godot-kotlin-tree] Warning: failed to scan .gdj files in $projectRoot"
            )
        }
        return result
    }

    internal fun parseGdj(content: String): Pair<String, GdjInfo>? {
        var relativeSourcePath: String? = null
        var fqName: String? = null
        var baseType: String? = null
        val supertypes = mutableListOf<String>()
        var inSupertypes = false

        for (raw in content.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("//")) continue

            when {
                inSupertypes -> {
                    if (line == "]") {
                        inSupertypes = false
                    } else {
                        val entry = line.trimEnd(',').trim()
                        if (entry.isNotEmpty()) supertypes.add(entry)
                    }
                }

                line.startsWith("relativeSourcePath = ") ->
                    relativeSourcePath = line.removePrefix("relativeSourcePath = ").trim()

                line.startsWith("fqName = ") ->
                    fqName = line.removePrefix("fqName = ").trim()

                line.startsWith("baseType = ") ->
                    baseType = line.removePrefix("baseType = ").trim()

                line.startsWith("supertypes = [") -> {
                    val after = line.removePrefix("supertypes = [")
                    if (after.contains("]")) {
                        val inner = after.substringBefore("]")
                        inner.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            .forEach { supertypes.add(it) }
                    } else {
                        inSupertypes = true
                    }
                }
            }
        }

        val key = relativeSourcePath ?: return null
        val info = GdjInfo(
            fqName = fqName ?: return null,
            baseType = baseType ?: return null,
            supertypes = supertypes,
        )
        return key to info
    }
}

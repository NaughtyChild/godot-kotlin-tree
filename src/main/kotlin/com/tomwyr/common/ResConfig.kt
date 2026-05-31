package com.tomwyr.common

import com.tomwyr.GodotKotlinTreeInput

object ResConfig {
    val DEFAULT_EXCLUDE_DIRS: Set<String> = setOf(
        "build",
        ".gradle",
        ".godot",
        "out",
        "jvm",
        "gdj",
        "src",
        ".idea",
    )

    fun resolveExcludeDirs(resExcludeDirs: List<String>): Set<String> =
        when {
            resExcludeDirs === GodotKotlinTreeInput.EXCLUDE_DIRS_DEFAULT_SENTINEL -> DEFAULT_EXCLUDE_DIRS
            resExcludeDirs.isEmpty() -> emptySet()
            else -> resExcludeDirs.map { it.trimEnd('/') }.toSet()
        }
}

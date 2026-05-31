package com.tomwyr.common

object ResConflictValidator {
    fun validate(entries: List<ResourceEntry>) {
        checkDuplicateSymbols(entries)
        checkDirectoryCollisions(entries)
    }

    private fun checkDuplicateSymbols(entries: List<ResourceEntry>) {
        val grouped = entries.groupBy { entry ->
            (entry.parentSegments + entry.constName).joinToString(".") { it.lowercase() }
        }
        for ((symbolPath, group) in grouped) {
            if (group.size > 1) {
                throw GeneratorError(
                    DuplicateResourceSymbol(
                        constPath = symbolPath,
                        conflictingResPaths = group.map { it.resPath }.sorted(),
                    ),
                )
            }
        }
    }

    private fun checkDirectoryCollisions(entries: List<ResourceEntry>) {
        val parentToSanitized = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()

        for (entry in entries) {
            var parentRaw = ""
            for (i in entry.rawDirSegments.indices) {
                val rawDir = entry.rawDirSegments[i]
                val sanitizedDir = entry.parentSegments[i + 1]
                val sanitizedKey = sanitizedDir.lowercase()
                parentToSanitized
                    .getOrPut(parentRaw) { mutableMapOf() }
                    .getOrPut(sanitizedKey) { mutableSetOf() }
                    .add(rawDir)
                parentRaw = if (parentRaw.isEmpty()) rawDir else "$parentRaw/$rawDir"
            }
        }

        for ((parentRaw, children) in parentToSanitized) {
            for ((_, rawNames) in children) {
                if (rawNames.size <= 1) continue
                val sanitizedNames = rawNames.map { IdentifierSanitizer.sanitizeDir(it) }.distinct()
                if (sanitizedNames.size == 1) {
                    val parentPath = if (parentRaw.isEmpty()) "res://" else "res://$parentRaw"
                    throw GeneratorError(
                        DirectoryNameCollision(
                            parentPath = parentPath,
                            conflictingDirNames = rawNames.sorted().toList(),
                        ),
                    )
                }
            }
        }
    }
}

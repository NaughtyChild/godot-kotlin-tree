package com.tomwyr.command

import com.tomwyr.common.ResourceEntry
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Atomically writes a single file without touching sibling files in the same directory.
 */
open class SingleFileWriter {
    fun write(target: Path, content: String) {
        val parent = target.parent ?: error("Target file must have a parent directory: $target")
        Files.createDirectories(parent)

        val staging = parent.resolve("${target.fileName}.staging")

        if (Files.exists(staging)) {
            Files.delete(staging)
        }

        try {
            renderFile(staging, content)
            moveAtomically(staging, target)
        } catch (e: Exception) {
            if (Files.exists(staging)) {
                Files.delete(staging)
            }
            throw e
        }
    }

    protected open fun renderFile(target: Path, content: String) {
        Files.writeString(target, content)
    }

    private fun moveAtomically(staging: Path, target: Path) {
        try {
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: Exception) {
            Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

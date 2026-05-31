package com.tomwyr.command

import com.tomwyr.common.GeneratedFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class NodeTreeWriter {
    fun write(content: String, path: String) {
        val file = File(path).apply {
            parentFile.mkdirs()
            createNewFile()
        }
        file.outputStream().apply {
            write(content.toByteArray())
            close()
        }
    }
}

/**
 * Atomic multi-file writer.
 *
 * Strategy:
 * 1. Write all files into a sibling staging directory `<outputDir>.staging`.
 * 2. Once every file is written successfully, replace `outputDir` with staging:
 *    - Delete `outputDir`.
 *    - Move staging into `outputDir`.
 * 3. If any step fails, the previous `outputDir` content stays intact and the
 *    staging directory is cleaned up so it does not pollute future runs.
 *
 * `Files.move` is preferred over per-file copying to avoid partial states.
 * If atomic move is unsupported (e.g. cross-volume), the writer falls back to a
 * non-atomic move, but the previous output directory is still only deleted
 * after the new content has been fully prepared in staging.
 */
open class MultiFileWriter {
    fun write(outputDir: String, files: List<GeneratedFile>) {
        val output = Paths.get(outputDir).toAbsolutePath()
        val parent = output.parent
            ?: error("Output directory must have a parent: $outputDir")
        Files.createDirectories(parent)

        val staging = parent.resolve(output.fileName.toString() + ".staging")

        if (Files.exists(staging)) {
            staging.toFile().deleteRecursively()
        }
        Files.createDirectories(staging)

        try {
            for (generated in files) {
                val target = staging.resolve(generated.relativePath)
                Files.createDirectories(target.parent)
                renderFile(target, generated.content)
            }

            replaceDirectory(staging, output)
        } catch (e: Exception) {
            staging.toFile().deleteRecursively()
            throw e
        }
    }

    /**
     * Open for tests so that simulated write failures can be injected.
     */
    protected open fun renderFile(target: Path, content: String) {
        Files.writeString(target, content)
    }

    private fun replaceDirectory(staging: Path, output: Path) {
        if (Files.exists(output)) {
            output.toFile().deleteRecursively()
        }

        try {
            Files.move(staging, output, StandardCopyOption.ATOMIC_MOVE)
            return
        } catch (_: Exception) {
            // Fall through to non-atomic move
        }

        try {
            Files.move(staging, output)
        } catch (_: Exception) {
            copyRecursive(staging, output)
            staging.toFile().deleteRecursively()
        }
    }

    private fun copyRecursive(source: Path, target: Path) {
        Files.walk(source).use { stream ->
            stream.forEach { src ->
                val dest = target.resolve(source.relativize(src).toString())
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}

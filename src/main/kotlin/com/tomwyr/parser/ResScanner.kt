package com.tomwyr.parser

import com.tomwyr.common.ExtensionGroupMapper
import com.tomwyr.common.GeneratorError
import com.tomwyr.common.IdentifierSanitizer
import com.tomwyr.common.IllegalIdentifierException
import com.tomwyr.common.InvalidResourceIdentifier
import com.tomwyr.common.ResourceEntry
import com.tomwyr.common.ScanningResourcesFailed
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object ResScanner {
    fun scan(
        projectPath: Path,
        validate: Boolean,
        extensions: List<String>,
        excludeDirs: Set<String>,
    ): List<ResourceEntry> {
        if (extensions.isEmpty()) return emptyList()

        val rootDir = ProjectScanner.resolveProjectRoot(projectPath, validate)
        val extensionSet = extensions.map { it.lowercase().trimStart('.') }.toSet()
        val entries = mutableListOf<ResourceEntry>()

        try {
            Files.walkFileTree(
                rootDir,
                object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (dir != rootDir && dir.fileName.toString() in excludeDirs) {
                            return FileVisitResult.SKIP_SUBTREE
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val fileName = file.fileName.toString()
                        if (fileName.endsWith(".import", ignoreCase = true) ||
                            fileName.endsWith(".uid", ignoreCase = true)
                        ) {
                            return FileVisitResult.CONTINUE
                        }

                        val ext = extensionOf(fileName)?.lowercase() ?: return FileVisitResult.CONTINUE
                        if (ext !in extensionSet) return FileVisitResult.CONTINUE

                        val relative = rootDir.relativize(file)
                        val resPath = "res://${relative.toString().replace('\\', '/')}"
                        val parentDir = file.parent ?: rootDir
                        val rawDirsFromRoot = if (parentDir == rootDir) {
                            emptyList()
                        } else {
                            rootDir.relativize(parentDir).map { it.toString() }
                        }

                        try {
                            val stemRaw = fileName.substringBeforeLast('.')
                            val stem = IdentifierSanitizer.sanitizeStem(stemRaw)
                            val extSegment = IdentifierSanitizer.sanitizeExt(ext)
                            val constName = "${stem}_$extSegment"
                            val sanitizedDirs = rawDirsFromRoot.map { IdentifierSanitizer.sanitizeDir(it) }
                            val group = ExtensionGroupMapper.group(ext)
                            val parentSegments = listOf(group) + sanitizedDirs

                            entries.add(
                                ResourceEntry(
                                    resPath = resPath,
                                    parentSegments = parentSegments,
                                    constName = constName,
                                    rawDirSegments = rawDirsFromRoot,
                                ),
                            )
                        } catch (e: IllegalIdentifierException) {
                            throw GeneratorError(
                                InvalidResourceIdentifier(resPath, e.reason),
                            )
                        }

                        return FileVisitResult.CONTINUE
                    }
                },
            )
        } catch (e: GeneratorError) {
            throw e
        } catch (e: IOException) {
            throw GeneratorError(ScanningResourcesFailed(rootDir.toString()))
        }

        return entries.sortedBy { it.resPath }
    }

    private fun extensionOf(fileName: String): String? {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0 || dot == fileName.lastIndex) return null
        return fileName.substring(dot + 1)
    }
}

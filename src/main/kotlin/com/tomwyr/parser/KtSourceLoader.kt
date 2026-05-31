package com.tomwyr.parser

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

data class KtClassInfo(
    val fqName: String,
    val simpleName: String,
)

/**
 * Resolves Kotlin script metadata (`fqName`, `simpleName`) on demand from `.kt` source files
 * referenced by `.tscn` scenes.
 *
 * Key contract: the lookup key is the path string from a `.tscn` `[ext_resource type="Script" path="res://X"]`
 * with the `res://` prefix stripped, e.g. `src/main/kotlin/com/example/game/Main.kt`.
 *
 * Limitations (first-version, recorded in plan):
 * - Only the first `@RegisterClass` declaration in a file is recognised.
 * - `typealias` parents are not resolved.
 * - Annotations or class declarations inside string templates are ignored for parser simplicity.
 */
class KtSourceLoader(private val projectRoot: Path) {
    private val cache = mutableMapOf<String, KtClassInfo?>()
    private val mutableWarnings = mutableListOf<String>()

    fun load(scriptRelativePath: String): KtClassInfo? {
        val key = normalize(scriptRelativePath)
        if (cache.containsKey(key)) return cache[key]

        val info = resolve(key)
        cache[key] = info
        return info
    }

    fun warnings(): List<String> = mutableWarnings.toList()

    private fun resolve(key: String): KtClassInfo? {
        val absolute = projectRoot.resolve(key)
        if (!Files.isRegularFile(absolute)) {
            mutableWarnings.add(
                "[godot-kotlin-tree] Warning: no Kotlin source found for script `$key` " +
                    "(resolved to `$absolute`)."
            )
            return null
        }

        val content = try {
            Files.readString(absolute)
        } catch (e: IOException) {
            mutableWarnings.add(
                "[godot-kotlin-tree] Warning: failed to read Kotlin source `$absolute`: ${e.message}"
            )
            return null
        }

        return parse(content, key)
    }

    private fun parse(content: String, key: String): KtClassInfo? {
        val stripped = stripComments(content)
        val packageName = parsePackage(stripped)
        val classes = collectRegisteredClasses(stripped)

        if (classes.isEmpty()) {
            mutableWarnings.add(
                "[godot-kotlin-tree] Warning: unable to parse @RegisterClass in `$key`."
            )
            return null
        }

        if (classes.size > 1) {
            mutableWarnings.add(
                "[godot-kotlin-tree] Warning: multiple @RegisterClass declarations in `$key`; " +
                    "using the first one (`${classes.first()}`)."
            )
        }

        val simpleName = classes.first()
        val fqName = if (packageName.isNullOrBlank()) {
            mutableWarnings.add(
                "[godot-kotlin-tree] Warning: missing `package` declaration in `$key`; " +
                    "fqName falls back to simple name `$simpleName`."
            )
            simpleName
        } else {
            "$packageName.$simpleName"
        }

        return KtClassInfo(fqName = fqName, simpleName = simpleName)
    }

    private fun normalize(path: String): String =
        path.replace('\\', '/').trim()

    companion object {
        private val PACKAGE_REGEX = Regex("""^\s*package\s+([\w.]+)\s*;?\s*$""")
        private val CLASS_NAME_REGEX = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""")

        internal fun parsePackage(source: String): String? {
            for (raw in source.lineSequence()) {
                val match = PACKAGE_REGEX.matchEntire(raw)
                if (match != null) return match.groupValues[1]
            }
            return null
        }

        /**
         * Walks the (comment-stripped) source line by line.
         * A class is "registered" when `@RegisterClass` appears either on a previous
         * (annotation-only) line or in front of the `class` keyword on the same line.
         */
        internal fun collectRegisteredClasses(source: String): List<String> {
            val results = mutableListOf<String>()
            var pendingRegister = false

            for (raw in source.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty()) continue

                val hasRegisterAnnotation = containsRegisterAnnotation(line)
                val classMatch = CLASS_NAME_REGEX.find(line)

                if (classMatch != null && (hasRegisterAnnotation || pendingRegister)) {
                    results.add(classMatch.groupValues[1])
                    pendingRegister = false
                    continue
                }

                if (hasRegisterAnnotation && classMatch == null) {
                    pendingRegister = true
                }
            }

            return results
        }

        private fun containsRegisterAnnotation(line: String): Boolean {
            val idx = line.indexOf("@RegisterClass")
            if (idx < 0) return false
            val after = idx + "@RegisterClass".length
            if (after < line.length) {
                val next = line[after]
                if (next.isLetterOrDigit() || next == '_') return false
            }
            return true
        }

        /**
         * Removes line comments (// ...) and block comments (slash-star ... star-slash,
         * including nested), preserving newlines so that line numbers and the `package`
         * regex still work.
         *
         * String literals are skipped to avoid stripping comment markers that appear
         * inside strings.
         */
        internal fun stripComments(source: String): String {
            val sb = StringBuilder(source.length)
            var i = 0
            var blockDepth = 0

            while (i < source.length) {
                val c = source[i]

                if (blockDepth > 0) {
                    if (c == '/' && i + 1 < source.length && source[i + 1] == '*') {
                        blockDepth++
                        i += 2
                        continue
                    }
                    if (c == '*' && i + 1 < source.length && source[i + 1] == '/') {
                        blockDepth--
                        i += 2
                        continue
                    }
                    if (c == '\n' || c == '\r') sb.append(c)
                    i++
                    continue
                }

                if (c == '/' && i + 1 < source.length) {
                    val next = source[i + 1]
                    if (next == '/') {
                        while (i < source.length && source[i] != '\n' && source[i] != '\r') i++
                        continue
                    }
                    if (next == '*') {
                        blockDepth = 1
                        i += 2
                        continue
                    }
                }

                if (c == '"') {
                    sb.append(c)
                    i++
                    val tripleQuoted = i + 1 < source.length && source[i] == '"' && source[i + 1] == '"'
                    if (tripleQuoted) {
                        sb.append('"').append('"')
                        i += 2
                        while (i < source.length) {
                            if (i + 2 < source.length && source[i] == '"' && source[i + 1] == '"' && source[i + 2] == '"') {
                                sb.append('"').append('"').append('"')
                                i += 3
                                break
                            }
                            sb.append(source[i])
                            i++
                        }
                    } else {
                        while (i < source.length) {
                            val ch = source[i]
                            sb.append(ch)
                            i++
                            if (ch == '\\' && i < source.length) {
                                sb.append(source[i])
                                i++
                                continue
                            }
                            if (ch == '"') break
                        }
                    }
                    continue
                }

                sb.append(c)
                i++
            }

            return sb.toString()
        }
    }
}

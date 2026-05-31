package com.tomwyr.common

/**
 * Sanitizes filesystem path segments (file names, extensions, directory names) into
 * valid PascalCase Kotlin identifiers.
 */
object IdentifierSanitizer {
    private val SPLIT_AS_STEM = Regex("[-_\\s.]+")
    private val SPLIT_AS_NAME = Regex("[-_\\s]+")
    private val NON_ALNUM = Regex("[^A-Za-z0-9]")

    private val KOTLIN_HARD_KEYWORDS: Set<String> = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
        "in", "interface", "is", "null", "object", "package", "return", "super", "this",
        "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
    )

    fun sanitizeStem(raw: String): String = sanitize(raw, splitOnDot = true)

    fun sanitizeExt(raw: String): String = sanitize(raw, splitOnDot = false)

    fun sanitizeDir(raw: String): String = sanitize(raw, splitOnDot = false)

    private fun sanitize(raw: String, splitOnDot: Boolean): String {
        val splitter = if (splitOnDot) SPLIT_AS_STEM else SPLIT_AS_NAME
        val tokens = splitter.split(raw).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            throw IllegalIdentifierException(raw, "produces empty identifier after splitting")
        }

        val pascal = tokens.joinToString(separator = "") { tokenToPascal(it) }

        if (pascal.isEmpty() || pascal.all { it == '_' }) {
            throw IllegalIdentifierException(raw, "contains no alphanumeric characters")
        }

        val prefixed = if (pascal.first().isDigit()) "R_$pascal" else pascal

        if (prefixed.lowercase() in KOTLIN_HARD_KEYWORDS) {
            throw IllegalIdentifierException(raw, "would collide with Kotlin keyword `$prefixed`")
        }

        return prefixed
    }

    private fun tokenToPascal(token: String): String {
        val cleaned = NON_ALNUM.replace(token, "_")
        if (cleaned.isEmpty() || cleaned.all { it == '_' }) return ""

        val digitBoundarySplit = Regex("(?<=\\d)(?=\\p{Alpha})|(?<=\\p{Alpha})(?=\\d)")
        val parts = if (digitBoundarySplit.containsMatchIn(cleaned)) {
            digitBoundarySplit.split(cleaned).filter { it.isNotEmpty() }
        } else {
            listOf(cleaned)
        }

        return parts.joinToString(separator = "") { part ->
            part.replaceFirstChar { ch -> if (ch.isLetter()) ch.uppercaseChar() else ch }
        }
    }
}

class IllegalIdentifierException(val raw: String, val reason: String) :
    RuntimeException("Cannot sanitize `$raw`: $reason")

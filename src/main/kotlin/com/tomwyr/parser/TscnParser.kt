package com.tomwyr.parser

data class ParsedTscn(
    val extResources: Map<String, ExtResourceRef>,
    val nodes: List<RawNode>,
)

data class ExtResourceRef(
    val id: String,
    val type: String,
    val path: String,
)

data class RawNode(
    val name: String,
    val type: String?,
    val parent: String?,
    val instanceExtId: String?,
    val scriptExtId: String?,
)

object TscnParser {
    private val extResourcePattern = Regex("""^ExtResource\(\s*"([^"]+)"\s*\)\s*$""")

    fun parse(content: String): ParsedTscn {
        val extResources = mutableMapOf<String, ExtResourceRef>()
        val nodes = mutableListOf<RawNode>()

        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!isSectionStart(line)) {
                i++
                continue
            }

            val header = parseSectionHeader(line)
            i++

            val bodyLines = mutableListOf<String>()
            while (i < lines.size && !isSectionStart(lines[i])) {
                bodyLines.add(lines[i])
                i++
            }

            if (header == null) continue

            when (header.tag) {
                "ext_resource" -> {
                    val id = header.attrs["id"] ?: continue
                    val type = header.attrs["type"] ?: continue
                    val path = header.attrs["path"] ?: continue
                    extResources[id] = ExtResourceRef(id = id, type = type, path = path)
                }

                "node" -> {
                    val name = header.attrs["name"] ?: continue
                    val type = header.attrs["type"]
                    val parent = header.attrs["parent"]
                    val instance = header.attrs["instance"]?.let { extractExtResourceId(it) }
                    val scriptExtId = parseScriptExtId(bodyLines)
                    nodes += RawNode(
                        name = name,
                        type = type,
                        parent = parent,
                        instanceExtId = instance,
                        scriptExtId = scriptExtId,
                    )
                }
            }
        }

        return ParsedTscn(extResources = extResources, nodes = nodes)
    }

    private fun isSectionStart(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("[") && trimmed.length > 1 && trimmed[1].isLetter()
    }

    private fun parseScriptExtId(bodyLines: List<String>): String? {
        for (line in bodyLines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("script = ")) {
                val value = trimmed.removePrefix("script = ").trim()
                return extractExtResourceId(value)
            }
        }
        return null
    }

    private fun extractExtResourceId(value: String): String? {
        return extResourcePattern.matchEntire(value.trim())?.groupValues?.get(1)
    }

    private data class SectionHeader(val tag: String, val attrs: Map<String, String>)

    private fun parseSectionHeader(rawLine: String): SectionHeader? {
        val line = rawLine.trimEnd('\r', '\n')
        if (line.isEmpty() || line[0] != '[') return null

        var i = 1
        val tagStart = i
        while (i < line.length && line[i] != ' ' && line[i] != ']') i++
        if (i == tagStart) return null
        val tag = line.substring(tagStart, i)

        if (tag !in KNOWN_TAGS) return null

        while (i < line.length && line[i] == ' ') i++

        val attrs = mutableMapOf<String, String>()
        while (i < line.length && line[i] != ']') {
            val keyStart = i
            while (i < line.length && line[i] != '=' && line[i] != ' ' && line[i] != ']') i++
            if (i >= line.length || line[i] != '=') {
                while (i < line.length && line[i] != ' ' && line[i] != ']') i++
                while (i < line.length && line[i] == ' ') i++
                continue
            }
            val key = line.substring(keyStart, i).trim()
            i++
            while (i < line.length && line[i] == ' ') i++

            val (value, newI) = readValue(line, i)
            i = newI
            if (key.isNotEmpty()) attrs[key] = value

            while (i < line.length && line[i] == ' ') i++
        }

        return SectionHeader(tag = tag, attrs = attrs)
    }

    private fun readValue(s: String, start: Int): Pair<String, Int> {
        if (start >= s.length) return "" to start
        return when (s[start]) {
            '"' -> readQuotedString(s, start)
            '[' -> readBalanced(s, start, '[', ']')
            '{' -> readBalanced(s, start, '{', '}')
            else -> readBareValue(s, start)
        }
    }

    private fun readQuotedString(s: String, start: Int): Pair<String, Int> {
        var i = start + 1
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i]
            when {
                c == '\\' && i + 1 < s.length -> {
                    sb.append(s[i + 1])
                    i += 2
                }
                c == '"' -> return sb.toString() to (i + 1)
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString() to i
    }

    private fun readBalanced(s: String, start: Int, open: Char, close: Char): Pair<String, Int> {
        var depth = 0
        var i = start
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i]
            if (c == '"') {
                val (qs, ni) = readQuotedString(s, i)
                sb.append('"').append(qs).append('"')
                i = ni
                continue
            }
            if (c == open) depth++
            else if (c == close) {
                depth--
                if (depth == 0) {
                    sb.append(c)
                    return sb.toString() to (i + 1)
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString() to i
    }

    private fun readBareValue(s: String, start: Int): Pair<String, Int> {
        var i = start
        var parenDepth = 0
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i]
            if (parenDepth == 0 && (c == ' ' || c == ']')) break
            if (c == '"') {
                val (qs, ni) = readQuotedString(s, i)
                sb.append('"').append(qs).append('"')
                i = ni
                continue
            }
            if (c == '(') parenDepth++
            else if (c == ')') parenDepth--
            sb.append(c)
            i++
        }
        return sb.toString() to i
    }

    private val KNOWN_TAGS = setOf("ext_resource", "node")
}

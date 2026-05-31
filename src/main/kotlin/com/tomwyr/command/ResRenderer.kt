package com.tomwyr.command

import com.tomwyr.common.ResourceEntry

class ResRenderer(private val packageName: String?) {

    fun render(entries: List<ResourceEntry>): String {
        val root = buildTrie(entries)
        return buildString {
            append(packageDecl())
            append("object Res {\n")
            renderNode(root, indent = 1, this)
            append("}\n")
        }
    }

    private fun packageDecl(): String =
        if (packageName != null) "package $packageName\n\n" else ""

    private fun buildTrie(entries: List<ResourceEntry>): TrieNode {
        val root = TrieNode()
        for (entry in entries) {
            var node = root
            for (segment in entry.parentSegments) {
                node = node.objects.getOrPut(segment) { TrieNode() }
            }
            node.consts[entry.constName] = entry.resPath
        }
        return root
    }

    private fun renderNode(node: TrieNode, indent: Int, out: StringBuilder) {
        val pad = "    ".repeat(indent)

        for (name in node.consts.keys.sorted()) {
            val path = node.consts.getValue(name)
            out.append("${pad}const val $name = \"$path\"\n")
        }

        for (name in node.objects.keys.sorted()) {
            out.append("${pad}object $name {\n")
            renderNode(node.objects.getValue(name), indent + 1, out)
            out.append("$pad}\n")
        }
    }

    private class TrieNode {
        val objects = linkedMapOf<String, TrieNode>()
        val consts = linkedMapOf<String, String>()
    }
}

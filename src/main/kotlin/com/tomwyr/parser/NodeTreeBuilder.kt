package com.tomwyr.parser

import com.tomwyr.common.GeneratorError
import com.tomwyr.common.LeafNode
import com.tomwyr.common.NestedScene
import com.tomwyr.common.Node
import com.tomwyr.common.NodeParams
import com.tomwyr.common.ParentNode
import com.tomwyr.common.ParentNodeNotFound
import com.tomwyr.common.UnexpectedNodeParameters
import com.tomwyr.common.UnexpectedSceneResource

object NodeTreeBuilder {
    fun build(parsed: ParsedTscn, sceneName: String): Node {
        val root = parsed.nodes.firstOrNull { it.parent == null }
            ?: throw GeneratorError(ParentNodeNotFound(sceneName))
        return buildSubtree(root, ".", parsed)
    }

    private fun buildSubtree(node: RawNode, childParentKey: String, parsed: ParsedTscn): Node {
        if (node.instanceExtId != null) {
            val ext = parsed.extResources[node.instanceExtId]
                ?: throw GeneratorError(UnexpectedSceneResource(node.instanceExtId))
            if (ext.type != "PackedScene") {
                throw GeneratorError(UnexpectedSceneResource(node.instanceExtId))
            }
            return NestedScene(name = node.name, scene = SceneName.fromResourcePath(ext.path))
        }

        val type = node.type
            ?: throw GeneratorError(UnexpectedNodeParameters(node.toParams()))

        val children = parsed.nodes.filter { it.parent == childParentKey }
        if (children.isEmpty()) {
            return LeafNode(name = node.name, type = type)
        }

        val childNodes = children.map { child ->
            val nextChildParent = if (childParentKey == ".") child.name else "$childParentKey/${child.name}"
            buildSubtree(child, nextChildParent, parsed)
        }
        return ParentNode(name = node.name, type = type, children = childNodes)
    }

    private fun RawNode.toParams(): NodeParams = NodeParams(
        name = name,
        type = type,
        instance = instanceExtId,
        parent = parent,
    )
}

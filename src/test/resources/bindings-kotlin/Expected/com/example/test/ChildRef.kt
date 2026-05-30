package com.example.test

import godot.api.Node
import godot.core.NodePath
import kotlin.reflect.KProperty

class ChildRef<T : Node>(private val relativePath: String) {
    fun get(thisRef: Node): T {
        val node = thisRef.getNode(NodePath(relativePath))
            ?: throw NodeNotFoundException(relativePath)
        @Suppress("UNCHECKED_CAST")
        return node as T
    }

    operator fun getValue(thisRef: Node, property: KProperty<*>): T = get(thisRef)
}

class NodeNotFoundException(path: String) : Exception("Node not found at path: $path")

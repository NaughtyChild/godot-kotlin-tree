package com.tomwyr.command

import com.tomwyr.common.BindingEntry
import com.tomwyr.common.BuildResult
import com.tomwyr.common.GeneratedFile
import com.tomwyr.common.MountInfo
import com.tomwyr.common.SceneInfo

class BindingsRenderer(private val packageName: String?) {

    companion object {
        internal fun needsKotlinImport(fqName: String, outputPackage: String?): Boolean {
            val lastDot = fqName.lastIndexOf('.')
            if (lastDot < 0) return false
            val typePackage = fqName.substring(0, lastDot)
            return when (outputPackage) {
                null -> true
                else -> typePackage != outputPackage
            }
        }

        internal fun renderKotlinImports(fqNames: Iterable<String>, outputPackage: String?): String {
            val imports = fqNames
                .filter { needsKotlinImport(it, outputPackage) }
                .distinct()
                .sorted()
            if (imports.isEmpty()) return ""
            return imports.joinToString(separator = "\n", postfix = "\n") { "import $it" }
        }
    }

    fun render(result: BuildResult): List<GeneratedFile> {
        val files = mutableListOf<GeneratedFile>()
        files.add(renderChildRef())
        result.mountInfos.forEach { files.add(renderBindings(it)) }
        result.sceneInfos.forEach { files.add(renderScene(it)) }
        return files
    }

    private fun packageDir(): String =
        packageName?.replace('.', '/') ?: ""

    private fun packageDecl(): String =
        if (packageName != null) "package $packageName\n\n" else ""

    private fun filePath(name: String): String {
        val dir = packageDir()
        return if (dir.isEmpty()) "$name.kt" else "$dir/$name.kt"
    }

    private fun renderChildRef(): GeneratedFile {
        val content = buildString {
            append(packageDecl())
            append("import godot.api.Node\n")
            append("import godot.core.NodePath\n")
            append("import kotlin.reflect.KProperty\n")
            append("\n")
            append("class ChildRef<T : Node>(private val relativePath: String) {\n")
            append("    fun get(thisRef: Node): T {\n")
            append("        val node = thisRef.getNode(NodePath(relativePath))\n")
            append("            ?: throw NodeNotFoundException(relativePath)\n")
            append("        @Suppress(\"UNCHECKED_CAST\")\n")
            append("        return node as T\n")
            append("    }\n")
            append("\n")
            append("    operator fun getValue(thisRef: Node, property: KProperty<*>): T = get(thisRef)\n")
            append("}\n")
            append("\n")
            append("class NodeNotFoundException(path: String) : Exception(\"Node not found at path: \$path\")\n")
        }
        return GeneratedFile(filePath("ChildRef"), content)
    }

    private fun renderBindings(mount: MountInfo): GeneratedFile {
        val kotlinImports = renderKotlinImports(
            mount.entries.mapNotNull { it.kotlinFqName },
            packageName,
        )
        val content = buildString {
            append(packageDecl())
            append(kotlinImports)
            if (mount.entries.any { isGodotType(it.type) }) {
                append("import godot.api.*\n")
            }
            if (kotlinImports.isNotEmpty() || mount.entries.any { isGodotType(it.type) }) {
                append("\n")
            }
            append("object ${mount.classSimpleName}Bindings {\n")
            for (entry in mount.entries) {
                append("    val ${entry.propertyName} = ChildRef<${entry.type}>(\"${entry.relativePath}\")\n")
            }
            if (mount.entries.isEmpty()) {
                append("    // No child nodes found\n")
            }
            append("}\n")
        }
        return GeneratedFile(filePath("${mount.classSimpleName}Bindings"), content)
    }

    private fun renderScene(scene: SceneInfo): GeneratedFile {
        val kotlinImports = renderKotlinImports(listOf(scene.classFqName), packageName)
        val content = buildString {
            append(packageDecl())
            append(kotlinImports)
            append("import godot.api.PackedScene\n")
            append("import godot.api.ResourceLoader\n")
            append("\n")
            append("object ${scene.sceneName}Scene {\n")
            append("    const val PATH = \"${scene.tscnResPath}\"\n")
            append("\n")
            append("    fun instantiate(): ${scene.classSimpleName} =\n")
            append("        (ResourceLoader.load(PATH) as PackedScene).instantiate() as ${scene.classSimpleName}\n")
            append("}\n")
        }
        return GeneratedFile(filePath("${scene.sceneName}Scene"), content)
    }

    private fun isGodotType(type: String): Boolean =
        !type.contains('.')
}

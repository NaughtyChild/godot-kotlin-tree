package com.tomwyr.parser

import com.tomwyr.common.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object MountTreeBuilder {
    fun build(
        tscnFiles: List<Path>,
        projectRoot: Path,
        gdjIndex: Map<String, GdjInfo>,
    ): BuildResult {
        val warnings = mutableListOf<String>()

        val parsedMap: Map<Path, ParsedTscn> = tscnFiles.associateWith { path ->
            try {
                TscnParser.parse(Files.readString(path))
            } catch (e: IOException) {
                throw GeneratorError(ReadingSceneFailed(path.toString()))
            }
        }

        val sceneRootTypes: Map<String, SceneRootInfo> = buildSceneRootTypeMap(parsedMap, projectRoot)

        val mountInfos = mutableListOf<MountInfo>()
        val sceneInfos = mutableListOf<SceneInfo>()

        for ((tscnPath, parsed) in parsedMap) {
            processScene(
                tscnPath, parsed, projectRoot, gdjIndex, sceneRootTypes, warnings,
                mountInfos, sceneInfos,
            )
        }

        validateMountTrees(mountInfos)

        return BuildResult(
            mountInfos = mountInfos.sortedBy { it.classSimpleName },
            sceneInfos = sceneInfos.sortedBy { it.sceneName },
            warnings = warnings,
        )
    }

    private fun buildSceneRootTypeMap(
        parsedMap: Map<Path, ParsedTscn>,
        projectRoot: Path,
    ): Map<String, SceneRootInfo> {
        val result = mutableMapOf<String, SceneRootInfo>()
        for ((tscnPath, parsed) in parsedMap) {
            val root = parsed.nodes.firstOrNull { it.parent == null } ?: continue
            val resPath = toResPath(tscnPath, projectRoot) ?: continue
            val godotType = root.type ?: "Node"
            val scriptResPath = root.scriptExtId?.let { id ->
                parsed.extResources[id]?.takeIf { it.type == "Script" }?.path
            }
            result[resPath] = SceneRootInfo(type = godotType, scriptResPath = scriptResPath)
        }
        return result
    }

    private fun processScene(
        tscnPath: Path,
        parsed: ParsedTscn,
        projectRoot: Path,
        gdjIndex: Map<String, GdjInfo>,
        sceneRootTypes: Map<String, SceneRootInfo>,
        warnings: MutableList<String>,
        mountInfos: MutableList<MountInfo>,
        sceneInfos: MutableList<SceneInfo>,
    ) {
        for (rawNode in parsed.nodes) {
            val scriptExtId = rawNode.scriptExtId ?: continue
            val extRes = parsed.extResources[scriptExtId] ?: continue
            if (extRes.type != "Script") continue
            val scriptResPath = extRes.path
            if (!scriptResPath.endsWith(".kt", ignoreCase = true)) continue

            if (rawNode.name.contains('%')) {
                throw GeneratorError(UnsupportedUniqueNodeName(rawNode.name))
            }

            val relativeSourcePath = scriptResPath.removePrefix("res://")
            val gdjInfo = gdjIndex[relativeSourcePath]
            if (gdjInfo == null) {
                warnings.add(
                    "[godot-kotlin-tree] Warning: no .gdj found for $scriptResPath. " +
                        "Run godot-kotlin-jvm compilation to generate .gdj files."
                )
                continue
            }

            val parentKey = nodeParentKey(rawNode)
            val entries = buildFlatEntries(
                parentKey, parsed, gdjIndex, sceneRootTypes, warnings,
            )

            mountInfos.add(
                MountInfo(
                    classSimpleName = gdjInfo.fqName.substringAfterLast('.'),
                    classFqName = gdjInfo.fqName,
                    entries = entries,
                )
            )

            if (rawNode.parent == null) {
                val resPath = toResPath(tscnPath, projectRoot)
                if (resPath != null) {
                    sceneInfos.add(
                        SceneInfo(
                            sceneName = SceneName.fromFile(tscnPath),
                            tscnResPath = resPath,
                            classSimpleName = gdjInfo.fqName.substringAfterLast('.'),
                            classFqName = gdjInfo.fqName,
                        )
                    )
                }
            }
        }
    }

    private fun nodeParentKey(node: RawNode): String {
        return when (node.parent) {
            null -> "."
            "." -> node.name
            else -> "${node.parent}/${node.name}"
        }
    }

    private fun buildFlatEntries(
        parentKey: String,
        parsed: ParsedTscn,
        gdjIndex: Map<String, GdjInfo>,
        sceneRootTypes: Map<String, SceneRootInfo>,
        warnings: MutableList<String>,
    ): List<BindingEntry> {
        val entries = mutableListOf<BindingEntry>()
        collectEntriesRecursive(
            parentKey = parentKey,
            relativePathPrefix = "",
            parsed = parsed,
            gdjIndex = gdjIndex,
            sceneRootTypes = sceneRootTypes,
            warnings = warnings,
            entries = entries,
        )
        return entries
    }

    private fun collectEntriesRecursive(
        parentKey: String,
        relativePathPrefix: String,
        parsed: ParsedTscn,
        gdjIndex: Map<String, GdjInfo>,
        sceneRootTypes: Map<String, SceneRootInfo>,
        warnings: MutableList<String>,
        entries: MutableList<BindingEntry>,
    ) {
        val children = parsed.nodes.filter { it.parent == parentKey }
        for (child in children) {
            if (child.name.contains('%')) {
                throw GeneratorError(UnsupportedUniqueNodeName(child.name))
            }

            val relativePath =
                if (relativePathPrefix.isEmpty()) child.name else "$relativePathPrefix/${child.name}"
            val propertyName = toPropertyName(relativePath)

            when {
                child.instanceExtId != null -> {
                    val instanceResPath = parsed.extResources[child.instanceExtId]?.path
                    val type = resolveInstanceType(instanceResPath, gdjIndex, sceneRootTypes, warnings)
                    entries.add(BindingEntry(propertyName, relativePath, type))
                }

                else -> {
                    val type = child.type ?: "Node"
                    entries.add(BindingEntry(propertyName, relativePath, type))

                    val childParentKey =
                        if (parentKey == ".") child.name else "$parentKey/${child.name}"
                    collectEntriesRecursive(
                        childParentKey, relativePath, parsed, gdjIndex, sceneRootTypes, warnings, entries,
                    )
                }
            }
        }
    }

    private fun resolveInstanceType(
        instanceResPath: String?,
        gdjIndex: Map<String, GdjInfo>,
        sceneRootTypes: Map<String, SceneRootInfo>,
        warnings: MutableList<String>,
    ): String {
        if (instanceResPath == null) return "Node"
        val rootInfo = sceneRootTypes[instanceResPath] ?: return "Node"

        val scriptResPath = rootInfo.scriptResPath
        if (scriptResPath != null) {
            val relSrc = scriptResPath.removePrefix("res://")
            val gdj = gdjIndex[relSrc]
            if (gdj != null) {
                return gdj.fqName.substringAfterLast('.')
            }
            warnings.add(
                "[godot-kotlin-tree] Warning: no .gdj found for $scriptResPath when resolving instance type."
            )
        }
        return rootInfo.type
    }

    private fun validateMountTrees(mountInfos: List<MountInfo>) {
        val byScript = mountInfos.groupBy { it.classFqName }
        for ((fqName, infos) in byScript) {
            if (infos.size < 2) continue
            val signature = subtreeSignature(infos.first().entries)
            for (other in infos.drop(1)) {
                if (subtreeSignature(other.entries) != signature) {
                    throw GeneratorError(IncompatibleMountTrees(fqName))
                }
            }
        }
    }

    private fun subtreeSignature(entries: List<BindingEntry>): String =
        entries.joinToString("|") { "${it.propertyName}:${it.type}" }

    private fun toPropertyName(relativePath: String): String =
        relativePath.split('/').joinToString("") { segment ->
            segment.split("\\s+".toRegex()).joinToString("") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        }

    private fun toResPath(tscnPath: Path, projectRoot: Path): String? =
        runCatching {
            "res://" + projectRoot.relativize(tscnPath).toString().replace('\\', '/')
        }.getOrNull()
}

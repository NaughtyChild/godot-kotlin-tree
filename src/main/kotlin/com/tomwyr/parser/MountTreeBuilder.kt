package com.tomwyr.parser

import com.tomwyr.common.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object MountTreeBuilder {
    fun build(
        tscnFiles: List<Path>,
        projectRoot: Path,
        ktLoader: KtSourceLoader,
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
                tscnPath, parsed, projectRoot, ktLoader, sceneRootTypes, warnings,
                mountInfos, sceneInfos,
            )
        }

        validateMountTrees(mountInfos)

        warnings += ktLoader.warnings()

        return BuildResult(
            mountInfos = mountInfos.sortedBy { it.classSimpleName },
            sceneInfos = sceneInfos.sortedBy { it.sceneName },
            warnings = warnings.distinct(),
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
        ktLoader: KtSourceLoader,
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
            val ktInfo = ktLoader.load(relativeSourcePath)
            if (ktInfo == null) {
                continue
            }

            val parentKey = nodeParentKey(rawNode)
            val entries = buildFlatEntries(
                parentKey, parsed, ktLoader, sceneRootTypes, warnings,
            )

            mountInfos.add(
                MountInfo(
                    classSimpleName = ktInfo.simpleName,
                    classFqName = ktInfo.fqName,
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
                            classSimpleName = ktInfo.simpleName,
                            classFqName = ktInfo.fqName,
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
        ktLoader: KtSourceLoader,
        sceneRootTypes: Map<String, SceneRootInfo>,
        warnings: MutableList<String>,
    ): List<BindingEntry> {
        val entries = mutableListOf<BindingEntry>()
        collectEntriesRecursive(
            parentKey = parentKey,
            relativePathPrefix = "",
            parsed = parsed,
            ktLoader = ktLoader,
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
        ktLoader: KtSourceLoader,
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
                    val resolved = resolveInstanceType(instanceResPath, ktLoader, sceneRootTypes)
                    entries.add(
                        BindingEntry(
                            propertyName = propertyName,
                            relativePath = relativePath,
                            type = resolved.simpleName,
                            kotlinFqName = resolved.kotlinFqName,
                        ),
                    )
                }

                else -> {
                    val type = child.type ?: "Node"
                    entries.add(BindingEntry(propertyName, relativePath, type, kotlinFqName = null))

                    val childParentKey =
                        if (parentKey == ".") child.name else "$parentKey/${child.name}"
                    collectEntriesRecursive(
                        childParentKey, relativePath, parsed, ktLoader, sceneRootTypes, warnings, entries,
                    )
                }
            }
        }
    }

    private data class ResolvedInstanceType(
        val simpleName: String,
        val kotlinFqName: String?,
    )

    private fun resolveInstanceType(
        instanceResPath: String?,
        ktLoader: KtSourceLoader,
        sceneRootTypes: Map<String, SceneRootInfo>,
    ): ResolvedInstanceType {
        if (instanceResPath == null) return ResolvedInstanceType("Node", null)
        val rootInfo = sceneRootTypes[instanceResPath] ?: return ResolvedInstanceType("Node", null)

        val scriptResPath = rootInfo.scriptResPath
        if (scriptResPath != null && scriptResPath.endsWith(".kt", ignoreCase = true)) {
            val relSrc = scriptResPath.removePrefix("res://")
            val ktInfo = ktLoader.load(relSrc)
            if (ktInfo != null) {
                return ResolvedInstanceType(ktInfo.simpleName, ktInfo.fqName)
            }
        }
        return ResolvedInstanceType(rootInfo.type, null)
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

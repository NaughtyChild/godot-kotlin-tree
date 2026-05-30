package com.tomwyr.common

import java.nio.file.Path

data class GdjInfo(
    val fqName: String,
    val baseType: String,
    val supertypes: List<String>,
)

data class BindingEntry(
    val propertyName: String,
    val relativePath: String,
    val type: String,
)

data class MountInfo(
    val classSimpleName: String,
    val classFqName: String,
    val entries: List<BindingEntry>,
)

data class SceneInfo(
    val sceneName: String,
    val tscnResPath: String,
    val classSimpleName: String,
    val classFqName: String,
)

data class BuildResult(
    val mountInfos: List<MountInfo>,
    val sceneInfos: List<SceneInfo>,
    val warnings: List<String>,
)

data class GeneratedFile(val relativePath: String, val content: String)

internal data class SceneRootInfo(val type: String, val scriptResPath: String?)

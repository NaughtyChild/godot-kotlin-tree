package com.tomwyr.common

sealed class GodotKotlinTreeError : Exception() {
    override fun getLocalizedMessage(): String = when (this) {
        is GeneratorError -> error.localizedMessage
    }
}

class GeneratorError(val error: GodotNodeTreeError) : GodotKotlinTreeError()

sealed class GodotNodeTreeError : Exception() {
    override fun getLocalizedMessage(): String = when (this) {
        is InvalidGodotProject -> "Godot project could not be found at path `$projectPath`."
        is ScanningScenesFailed -> "Unable to scan scene files for project at `$projectPath`."
        is ReadingSceneFailed -> "Unable to read contents of scene at `$scenePath`."
        is UnexpectedNodeParameters -> "A node with unexpected set of parameters encountered: $nodeParams."
        is UnexpectedSceneResource -> "A node pointing to an unknown scene resource encountered with id: $instance."
        is ParentNodeNotFound -> "None of the parsed nodes was identified as the parent node of scene $sceneName."
        is ScriptTypeMismatch ->
            "Node `$nodeName` has type `$tscnType` in .tscn but this is not in the supertypes of `$scriptPath`. " +
                "Expected one of: ${supertypes.joinToString()}."
        is IncompatibleMountTrees ->
            "Script `$scriptPath` is attached to multiple nodes with incompatible subtree structures."
        is UnsupportedUniqueNodeName ->
            "Node `$nodeName` uses the `%` unique-name syntax which is not yet supported."
        is UnresolvedScriptClass ->
            "No .gdj file found for script `$scriptPath`. Run godot-kotlin-jvm compilation to generate it."
    }
}

class InvalidGodotProject(val projectPath: String) : GodotNodeTreeError()

class ScanningScenesFailed(val projectPath: String) : GodotNodeTreeError()

class ReadingSceneFailed(val scenePath: String) : GodotNodeTreeError()

class UnexpectedNodeParameters(val nodeParams: NodeParams) : GodotNodeTreeError()

class UnexpectedSceneResource(val instance: String) : GodotNodeTreeError()

class ParentNodeNotFound(val sceneName: String) : GodotNodeTreeError()

class ScriptTypeMismatch(
    val nodeName: String,
    val tscnType: String,
    val scriptPath: String,
    val supertypes: List<String>,
) : GodotNodeTreeError()

class IncompatibleMountTrees(val scriptPath: String) : GodotNodeTreeError()

class UnsupportedUniqueNodeName(val nodeName: String) : GodotNodeTreeError()

class UnresolvedScriptClass(val scriptPath: String) : GodotNodeTreeError()

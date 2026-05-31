package com.tomwyr.common

/**
 * One entry per scanned resource file.
 *
 * @property resPath        Godot resource path, e.g. `res://assets/hit.wav`.
 * @property parentSegments Path of nested `object` names from `Res` down to the parent of the
 *                          generated `const val`. Each segment is already PascalCase-sanitized.
 *                          For `res://assets/sfx/hit.wav` this is `["Sound", "Assets", "Sfx"]`.
 *                          For a root-level file `res://hit.wav` this is `["Sound"]` (no `Root` layer).
 * @property constName      Generated leaf `const val` name, formed as `{Stem}_{Ext}` with both
 *                          parts PascalCase (e.g. `Hit_Wav`, `CoinFrame5_Png`).
 */
data class ResourceEntry(
    val resPath: String,
    val parentSegments: List<String>,
    val constName: String,
    /** Original directory name segments relative to the project root (unsanitized). */
    val rawDirSegments: List<String>,
)

data class ResBuildResult(val entries: List<ResourceEntry>)

package com.tomwyr.common

/**
 * Maps a file extension (lowercase, no leading dot) to the top-level group `object`
 * that wraps the generated `const val`. Extensions listed in `resExtensions` but not
 * mapped here are routed to [UNKNOWN_GROUP].
 */
object ExtensionGroupMapper {
    private const val UNKNOWN_GROUP = "Unknown"

    private val mapping: Map<String, String> = mapOf(
        "wav" to "Sound",
        "mp3" to "Sound",
        "ogg" to "Sound",
        "png" to "Image",
        "jpg" to "Image",
        "jpeg" to "Image",
        "webp" to "Image",
        "svg" to "Image",
        "ttf" to "Font",
        "otf" to "Font",
        "tres" to "Resource",
        "gdshader" to "Shader",
    )

    fun group(extensionLowercase: String): String =
        mapping[extensionLowercase] ?: UNKNOWN_GROUP
}

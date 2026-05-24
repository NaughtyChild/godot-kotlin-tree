package com.tomwyr.parser

import com.tomwyr.common.GeneratorError
import com.tomwyr.common.NodeTree
import com.tomwyr.common.ReadingSceneFailed
import com.tomwyr.common.Scene
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

object SceneTreeGenerator {
    fun generate(projectPath: String, validate: Boolean): NodeTree {
        val path = Paths.get(projectPath)
        val files = ProjectScanner.findTscnFiles(path, validate)

        val scenes = files
            .map { file ->
                val content = try {
                    Files.readString(file)
                } catch (e: IOException) {
                    throw GeneratorError(ReadingSceneFailed(file.toString()))
                }
                val sceneName = SceneName.fromFile(file)
                val parsed = TscnParser.parse(content)
                Scene(name = sceneName, root = NodeTreeBuilder.build(parsed, sceneName))
            }
            .sortedBy { it.name }

        return NodeTree(scenes = scenes)
    }
}

package com.tomwyr.command

import com.tomwyr.common.GeneratedFile
import java.io.File

class NodeTreeWriter {
    fun write(content: String, path: String) {
        val file = File(path).apply {
            parentFile.mkdirs()
            createNewFile()
        }
        file.outputStream().apply {
            write(content.toByteArray())
            close()
        }
    }
}

class MultiFileWriter {
    fun write(outputDir: String, files: List<GeneratedFile>) {
        val dir = File(outputDir)
        if (dir.exists()) dir.deleteRecursively()
        dir.mkdirs()

        for (generated in files) {
            val target = File(outputDir, generated.relativePath)
            target.parentFile.mkdirs()
            target.writeText(generated.content)
        }
    }
}

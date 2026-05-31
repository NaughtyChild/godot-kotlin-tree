package com.tomwyr.command

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SingleFileWriterTest {
    private lateinit var workDir: Path

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("single-file-writer-test")
    }

    @AfterTest
    fun tearDown() {
        workDir.toFile().deleteRecursively()
    }

    @Test
    fun `writes target file`() {
        val target = workDir.resolve("out/Res.kt")
        SingleFileWriter().write(target, "object Res {}\n")
        assertEquals("object Res {}\n", Files.readString(target))
    }

    @Test
    fun `does not remove sibling files in the same directory`() {
        val dir = workDir.resolve("out/com/example/test")
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("MainBindings.kt"), "legacy bindings")

        SingleFileWriter().write(dir.resolve("Res.kt"), "object Res {}\n")

        assertEquals("legacy bindings", Files.readString(dir.resolve("MainBindings.kt")))
        assertEquals("object Res {}\n", Files.readString(dir.resolve("Res.kt")))
        assertTrue(!Files.exists(dir.resolve("Res.kt.staging")))
    }
}

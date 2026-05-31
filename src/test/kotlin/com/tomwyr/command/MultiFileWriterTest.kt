package com.tomwyr.command

import com.tomwyr.common.GeneratedFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class MultiFileWriterTest {
    private lateinit var workDir: Path

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("multi-file-writer-test")
    }

    @AfterTest
    fun tearDown() {
        workDir.toFile().deleteRecursively()
    }

    @Test
    fun `writes files into output directory`() {
        val output = workDir.resolve("out")

        MultiFileWriter().write(
            output.toString(),
            listOf(
                GeneratedFile("a.txt", "hello"),
                GeneratedFile("nested/b.txt", "world"),
            ),
        )

        assertEquals("hello", Files.readString(output.resolve("a.txt")))
        assertEquals("world", Files.readString(output.resolve("nested/b.txt")))
        assertFalse(Files.exists(workDir.resolve("out.staging")))
    }

    @Test
    fun `replaces existing output directory entirely`() {
        val output = workDir.resolve("out")
        Files.createDirectories(output)
        Files.writeString(output.resolve("legacy.txt"), "old")

        MultiFileWriter().write(
            output.toString(),
            listOf(GeneratedFile("fresh.txt", "new")),
        )

        assertFalse(Files.exists(output.resolve("legacy.txt")))
        assertEquals("new", Files.readString(output.resolve("fresh.txt")))
    }

    @Test
    fun `keeps old output and cleans staging when write fails`() {
        val output = workDir.resolve("out")
        Files.createDirectories(output)
        Files.writeString(output.resolve("legacy.txt"), "old")

        val staging = workDir.resolve("out.staging")

        val brokenWriter = object : MultiFileWriter() {
            override fun renderFile(target: Path, content: String) {
                if (target.fileName.toString() == "boom.txt") {
                    throw RuntimeException("simulated failure")
                }
                super.renderFile(target, content)
            }
        }

        try {
            brokenWriter.write(
                output.toString(),
                listOf(
                    GeneratedFile("good.txt", "ok"),
                    GeneratedFile("boom.txt", "explode"),
                ),
            )
            fail("Expected RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("simulated failure", e.message)
        }

        assertTrue(Files.exists(output.resolve("legacy.txt")))
        assertEquals("old", Files.readString(output.resolve("legacy.txt")))
        assertFalse(Files.exists(staging))
    }
}

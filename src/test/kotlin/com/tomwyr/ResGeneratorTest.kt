package com.tomwyr

import com.tomwyr.command.GenerateResCommand
import com.tomwyr.common.DuplicateResourceSymbol
import com.tomwyr.common.GeneratorError
import com.tomwyr.common.InvalidResourceIdentifier
import com.tomwyr.common.ResConflictValidator
import com.tomwyr.common.ResourceEntry
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ResGeneratorTest {
    @Test
    fun `basic wav asset`() {
        resTest("basic", listOf("wav"))
    }

    @Test
    fun `same stem multiple extensions`() {
        resTest("multi-ext", listOf("wav", "mp3"))
    }

    @Test
    fun `cross directory same file name`() {
        resTest("cross-dir", listOf("wav"))
    }

    @Test
    fun `nested directory`() {
        resTest("nested", listOf("wav"))
    }

    @Test
    fun `root level file`() {
        resTest("root-file", listOf("wav"))
    }

    @Test
    fun `stem tokenization`() {
        resTest("stem-split", listOf("png"))
    }

    @Test
    fun `unknown extension maps to Unknown group`() {
        resTest("unknown-ext", listOf("xyz"))
    }

    @Test
    fun `empty resExtensions generates empty Res object`() {
        resTest("empty-extensions", emptyList())
    }

    @Test
    fun `duplicate symbol paths fail validation`() {
        val entries = listOf(
            ResourceEntry(
                resPath = "res://assets/a.wav",
                parentSegments = listOf("Sound", "Assets"),
                constName = "Hit_Wav",
                rawDirSegments = listOf("assets"),
            ),
            ResourceEntry(
                resPath = "res://assets/b.wav",
                parentSegments = listOf("Sound", "Assets"),
                constName = "Hit_Wav",
                rawDirSegments = listOf("assets"),
            ),
        )

        val error = assertFailsWith<GeneratorError> {
            ResConflictValidator.validate(entries)
        }
        assertIs<DuplicateResourceSymbol>(error.error)
    }

    @Test
    fun `kotlin keyword directory fails during scan`() {
        val command = setUpResCommand("keyword-dir", listOf("wav"))
        val error = assertFailsWith<GeneratorError> { command.run() }
        assertIs<InvalidResourceIdentifier>(error.error)
    }
}

private const val packageName = "com.example.test"
private const val resBasePath = "src/test/resources/res-generation"

private fun resTest(testCase: String, extensions: List<String>) {
    val actualRoot = File("$resBasePath/$testCase/Actual")
    try {
        setUpResCommand(testCase, extensions).run()
        assertResOutputEqual(testCase)
    } finally {
        actualRoot.deleteRecursively()
    }
}

private fun setUpResCommand(testCase: String, extensions: List<String>): GenerateResCommand {
    val pkgPath = packageName.replace('.', '/')
    return GenerateResCommand(
        projectPath = "$resBasePath/$testCase",
        validateProjectPath = false,
        outputFile = Paths.get("$resBasePath/$testCase/Actual/$pkgPath/Res.kt"),
        packageName = packageName,
        resExtensions = extensions,
        resExcludeDirs = GodotKotlinTreeInput.EXCLUDE_DIRS_DEFAULT_SENTINEL,
    )
}

private fun assertResOutputEqual(testCase: String) {
    val expected = File("$resBasePath/$testCase/Expected/${packageName.replace('.', '/')}/Res.kt")
    val actual = File("$resBasePath/$testCase/Actual/${packageName.replace('.', '/')}/Res.kt")
    assertTrue(actual.exists(), "Expected generated Res.kt for $testCase")
    assertEquals(
        expected.readText(Charsets.UTF_8).replace("\r\n", "\n"),
        actual.readText(Charsets.UTF_8).replace("\r\n", "\n"),
        "Content mismatch for $testCase",
    )
}

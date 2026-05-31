package com.tomwyr

import com.tomwyr.command.GenerateTreeCommand
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratorTest {
    @Test
    fun `simple scene with kotlin script`() {
        test("bindings-kotlin", "com.example.test")
    }

    @Test
    fun `bindings can be generated without any gdj directory`() {
        test("bindings-kotlin-no-gdj", "com.example.test")
    }
}

fun test(testCase: String, packageName: String) {
    val actualRoot = File("$basePath/$testCase/Actual")
    try {
        val command = setUpTestCommand(testCase, packageName)
        command.run()
        assertOutputsEqual(testCase, packageName)
    } finally {
        actualRoot.deleteRecursively()
    }
}

const val basePath = "src/test/resources"

fun setUpTestCommand(testCase: String, packageName: String): GenerateTreeCommand {
    return GenerateTreeCommand(
        projectPath = "$basePath/$testCase",
        validateProjectPath = false,
        outputDir = "$basePath/$testCase/Actual",
        packageName = packageName,
    )
}

fun assertOutputsEqual(testCase: String, packageName: String) {
    val packageDir = packageName.replace('.', '/')
    val expectedDir = File("$basePath/$testCase/Expected/$packageDir")
    val actualDir = File("$basePath/$testCase/Actual/$packageDir")

    val expectedFiles = expectedDir.walkTopDown().filter { it.isFile }.sortedBy { it.name }
    val actualFiles = actualDir.walkTopDown().filter { it.isFile }.sortedBy { it.name }

    assertEquals(
        expectedFiles.map { it.name }.toSet(),
        actualFiles.map { it.name }.toSet(),
        "Generated file list mismatch for $testCase",
    )

    for (expectedFile in expectedFiles) {
        val actualFile = File(actualDir, expectedFile.name)
        val expected = expectedFile.readText(Charsets.UTF_8).replace("\r\n", "\n")
        val actual = actualFile.readText(Charsets.UTF_8).replace("\r\n", "\n")
        assertEquals(expected, actual, "Content mismatch for ${expectedFile.name}")
    }
}

fun cleanUpGeneratedOutput(testCase: String) {
    File("$basePath/$testCase/Actual").deleteRecursively()
}

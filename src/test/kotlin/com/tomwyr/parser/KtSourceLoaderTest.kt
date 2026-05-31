package com.tomwyr.parser

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtSourceLoaderTest {
    private lateinit var projectRoot: Path

    @BeforeTest
    fun setUp() {
        projectRoot = Files.createTempDirectory("kt-source-loader-test")
    }

    @AfterTest
    fun tearDown() {
        projectRoot.toFile().deleteRecursively()
    }

    @Test
    fun `parses a standard package and class`() {
        writeKt(
            "src/main/kotlin/com/example/Main.kt",
            """
            package com.example

            import godot.annotation.RegisterClass
            import godot.api.Node2D

            @RegisterClass
            class Main : Node2D() {}
            """.trimIndent(),
        )

        val info = KtSourceLoader(projectRoot).load("src/main/kotlin/com/example/Main.kt")

        assertEquals("com.example.Main", info?.fqName)
        assertEquals("Main", info?.simpleName)
    }

    @Test
    fun `accepts annotation on a separate line above the class`() {
        writeKt(
            "Main.kt",
            """
            package com.example

            @RegisterClass
            @SomeOther
            class Main(
                private val foo: Int,
            ) : Node2D() {}
            """.trimIndent(),
        )

        val info = KtSourceLoader(projectRoot).load("Main.kt")

        assertEquals("com.example.Main", info?.fqName)
    }

    @Test
    fun `accepts inline annotation in the class declaration line`() {
        writeKt(
            "Main.kt",
            """
            package com.example

            @RegisterClass class Main : Node2D() {}
            """.trimIndent(),
        )

        val info = KtSourceLoader(projectRoot).load("Main.kt")

        assertEquals("com.example.Main", info?.fqName)
    }

    @Test
    fun `ignores RegisterClass inside line comments`() {
        writeKt(
            "Main.kt",
            """
            package com.example

            // @RegisterClass class FakeOne : Node()
            class NotRegistered

            @RegisterClass
            class Real : Node2D()
            """.trimIndent(),
        )

        val info = KtSourceLoader(projectRoot).load("Main.kt")

        assertEquals("com.example.Real", info?.fqName)
    }

    @Test
    fun `ignores RegisterClass inside block comments`() {
        writeKt(
            "Main.kt",
            """
            package com.example

            /*
              @RegisterClass class Fake : Node()
            */

            @RegisterClass
            class Real : Node2D()
            """.trimIndent(),
        )

        val info = KtSourceLoader(projectRoot).load("Main.kt")

        assertEquals("com.example.Real", info?.fqName)
    }

    @Test
    fun `picks the first RegisterClass when multiple are present`() {
        writeKt(
            "Main.kt",
            """
            package com.example

            @RegisterClass
            class First : Node()

            @RegisterClass
            class Second : Node()
            """.trimIndent(),
        )

        val loader = KtSourceLoader(projectRoot)
        val info = loader.load("Main.kt")

        assertEquals("com.example.First", info?.fqName)
        assertEquals("First", info?.simpleName)
        assertTrue(loader.warnings().any { it.contains("multiple @RegisterClass") })
    }

    @Test
    fun `returns null with warning when file is missing`() {
        val loader = KtSourceLoader(projectRoot)
        val info = loader.load("does/not/exist/Main.kt")

        assertNull(info)
        assertTrue(loader.warnings().any { it.contains("no Kotlin source") })
    }

    @Test
    fun `falls back to simple name when package is missing`() {
        writeKt(
            "Main.kt",
            """
            @RegisterClass
            class Loose : Node2D()
            """.trimIndent(),
        )

        val loader = KtSourceLoader(projectRoot)
        val info = loader.load("Main.kt")

        assertEquals("Loose", info?.fqName)
        assertEquals("Loose", info?.simpleName)
        assertTrue(loader.warnings().any { it.contains("missing `package`") })
    }

    @Test
    fun `caches results across repeated calls`() {
        writeKt(
            "Main.kt",
            """
            package com.example
            @RegisterClass class Main : Node2D()
            """.trimIndent(),
        )

        val loader = KtSourceLoader(projectRoot)
        val first = loader.load("Main.kt")
        val second = loader.load("Main.kt")

        assertEquals(first, second)
        assertEquals("com.example.Main", first?.fqName)
    }

    @Test
    fun `normalises backslash-separated keys`() {
        writeKt(
            "src/main/kotlin/com/example/Main.kt",
            """
            package com.example
            @RegisterClass class Main : Node2D()
            """.trimIndent(),
        )

        val info = KtSourceLoader(projectRoot)
            .load("src\\main\\kotlin\\com\\example\\Main.kt")

        assertEquals("com.example.Main", info?.fqName)
    }

    private fun writeKt(relativePath: String, content: String) {
        val file = projectRoot.resolve(relativePath)
        file.parent?.createDirectories()
        Files.writeString(file, content)
    }
}

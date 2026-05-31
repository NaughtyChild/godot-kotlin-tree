package com.tomwyr.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BindingsRendererImportsTest {

    @Test
    fun `needs import when script is in subpackage`() {
        assertTrue(BindingsRenderer.needsKotlinImport("godot.tank.enemy.Enemy", "godot.tank"))
    }

    @Test
    fun `no import when script is in output package`() {
        assertFalse(BindingsRenderer.needsKotlinImport("godot.tank.Main", "godot.tank"))
    }

    @Test
    fun `no import when fq name has no package`() {
        assertFalse(BindingsRenderer.needsKotlinImport("Main", "godot.tank"))
    }

    @Test
    fun `needs import when output package is null`() {
        assertTrue(BindingsRenderer.needsKotlinImport("com.foo.Bar", null))
    }

    @Test
    fun `renderKotlinImports deduplicates and sorts`() {
        val result = BindingsRenderer.renderKotlinImports(
            listOf(
                "godot.tank.enemy.Enemy",
                "godot.tank.enemy.Enemy",
                "godot.tank.ui.Hud",
            ),
            "godot.tank",
        )
        assertEquals(
            "import godot.tank.enemy.Enemy\nimport godot.tank.ui.Hud\n",
            result,
        )
    }

    @Test
    fun `renderKotlinImports empty when all types are in output package`() {
        assertEquals("", BindingsRenderer.renderKotlinImports(listOf("godot.tank.Main"), "godot.tank"))
    }
}

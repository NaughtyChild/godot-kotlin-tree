package com.tomwyr.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdentifierSanitizerTest {
    @Test
    fun `sanitizeStem splits on hyphen and underscore`() {
        assertEquals("CoinFrame5", IdentifierSanitizer.sanitizeStem("coin-frame-5"))
        assertEquals("Hit", IdentifierSanitizer.sanitizeStem("hit"))
        assertEquals("Hit", IdentifierSanitizer.sanitizeStem("Hit"))
    }

    @Test
    fun `sanitizeExt capitalises extension`() {
        assertEquals("Wav", IdentifierSanitizer.sanitizeExt("wav"))
        assertEquals("Mp3", IdentifierSanitizer.sanitizeExt("mp3"))
        assertEquals("Gdshader", IdentifierSanitizer.sanitizeExt("gdshader"))
    }

    @Test
    fun `sanitizeDir capitalises directory segment`() {
        assertEquals("Assets", IdentifierSanitizer.sanitizeDir("assets"))
        assertEquals("Sfx", IdentifierSanitizer.sanitizeDir("sfx"))
    }

    @Test
    fun `numeric prefix applied when stem starts with digit`() {
        assertEquals("R_5Coins", IdentifierSanitizer.sanitizeStem("5coins"))
    }

    @Test
    fun `kotlin keyword rejected`() {
        assertFailsWith<IllegalIdentifierException> {
            IdentifierSanitizer.sanitizeDir("object")
        }
    }

    @Test
    fun `empty identifier rejected`() {
        assertFailsWith<IllegalIdentifierException> {
            IdentifierSanitizer.sanitizeStem("---")
        }
    }
}

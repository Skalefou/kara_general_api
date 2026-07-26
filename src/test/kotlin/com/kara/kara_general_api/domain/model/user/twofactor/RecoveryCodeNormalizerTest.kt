package com.kara.kara_general_api.domain.model.user.twofactor

import kotlin.test.Test
import kotlin.test.assertEquals

class RecoveryCodeNormalizerTest {
    @Test
    fun `should lowercase the code`() {
        assertEquals("cascade tulipe", RecoveryCodeNormalizer.normalize("Cascade TULIPE"))
    }

    @Test
    fun `should trim surrounding whitespace`() {
        assertEquals("cascade tulipe", RecoveryCodeNormalizer.normalize("   cascade tulipe  "))
    }

    @Test
    fun `should strip diacritics added by a predictive keyboard`() {
        assertEquals("cascade tulipe ecureuil", RecoveryCodeNormalizer.normalize("cascade tulipé écureuil"))
    }

    @Test
    fun `should turn hyphens and underscores into spaces`() {
        assertEquals("cascade tulipe marteau", RecoveryCodeNormalizer.normalize("cascade-tulipe_marteau"))
    }

    @Test
    fun `should collapse repeated separators into a single space`() {
        assertEquals("cascade tulipe", RecoveryCodeNormalizer.normalize("cascade   \t tulipe"))
    }

    @Test
    fun `should map every accepted variation of one code onto the same canonical form`() {
        val canonical = "cascade tulipe marteau renard"
        val variations =
            listOf(
                "Cascade Tulipe Marteau Renard",
                "  cascade-tulipe-marteau-renard ",
                "CASCADE_TULIPE_MARTEAU_RENARD",
                "cascade  tulipé   marteau\trenard",
            )

        variations.forEach { assertEquals(canonical, RecoveryCodeNormalizer.normalize(it)) }
    }

    @Test
    fun `should leave an already canonical code untouched`() {
        assertEquals(
            "cascade tulipe marteau renard",
            RecoveryCodeNormalizer.normalize("cascade tulipe marteau renard"),
        )
    }
}

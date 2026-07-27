package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WordlistRecoveryCodeGeneratorTest {
    private val sut = WordlistRecoveryCodeGenerator()

    @Test
    fun `should generate exactly the requested number of codes`() {
        val codes = sut.generate(count = 10, wordsPerCode = 4)

        assertEquals(10, codes.size)
    }

    @Test
    fun `should generate codes made of the requested number of words`() {
        val codes = sut.generate(count = 10, wordsPerCode = 4)

        assertTrue(codes.all { it.split(" ").size == 4 }, "codes attendus à 4 mots : $codes")
    }

    @Test
    fun `should generate distinct codes within one batch`() {
        // 1259 mots ^ 4 : une collision sur 10 tirages est astronomiquement improbable.
        val codes = sut.generate(count = 10, wordsPerCode = 4)

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `should generate a different batch on every call`() {
        assertTrue(sut.generate(10, 4).toSet() != sut.generate(10, 4).toSet())
    }

    @Test
    fun `should generate codes already in normalized form`() {
        val codes = sut.generate(count = 20, wordsPerCode = 4)

        codes.forEach { assertEquals(it, RecoveryCodeNormalizer.normalize(it)) }
    }

    @Test
    fun `should generate codes made only of lowercase unaccented letters and single spaces`() {
        val codes = sut.generate(count = 20, wordsPerCode = 4)

        codes.forEach { assertTrue(Regex("^[a-z]+( [a-z]+){3}$").matches(it), "code inattendu : $it") }
    }

    @Test
    fun `should reject a non-positive number of codes`() {
        assertFailsWith<IllegalArgumentException> { sut.generate(count = 0, wordsPerCode = 4) }
    }

    @Test
    fun `should reject a code without any word`() {
        assertFailsWith<IllegalArgumentException> { sut.generate(count = 10, wordsPerCode = 0) }
    }
}

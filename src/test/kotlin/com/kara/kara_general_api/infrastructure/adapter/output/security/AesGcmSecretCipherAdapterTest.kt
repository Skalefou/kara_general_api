package com.kara.kara_general_api.infrastructure.adapter.output.security

import java.security.SecureRandom
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AesGcmSecretCipherAdapterTest {
    private val validKey = base64Key(32)
    private val sut = AesGcmSecretCipherAdapter(validKey)

    @Test
    fun `should return the original secret when encrypting then decrypting`() {
        val secret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP"

        val roundTripped = sut.decrypt(sut.encrypt(secret))

        assertEquals(secret, roundTripped)
    }

    @Test
    fun `should produce a different ciphertext on every call thanks to the random iv`() {
        val secret = "JBSWY3DPEHPK3PXP"

        val first = sut.encrypt(secret)
        val second = sut.encrypt(secret)

        assertNotEquals(first, second)
        assertEquals(secret, sut.decrypt(first))
        assertEquals(secret, sut.decrypt(second))
    }

    @Test
    fun `should fail fast when the key is missing`() {
        val failure = assertFailsWith<IllegalArgumentException> { AesGcmSecretCipherAdapter("") }

        assertEquals(true, failure.message?.contains("TWO_FACTOR_ENCRYPTION_KEY"))
    }

    @Test
    fun `should fail fast when the key does not hold exactly 32 bytes`() {
        val failure = assertFailsWith<IllegalArgumentException> { AesGcmSecretCipherAdapter(base64Key(16)) }

        assertEquals(true, failure.message?.contains("32 octets"))
    }

    @Test
    fun `should reject a ciphertext too short to hold an iv`() {
        val truncated = Base64.getEncoder().encodeToString(ByteArray(8))

        assertFailsWith<IllegalArgumentException> { sut.decrypt(truncated) }
    }

    private fun base64Key(sizeInBytes: Int): String {
        val bytes = ByteArray(sizeInBytes).also(SecureRandom()::nextBytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}

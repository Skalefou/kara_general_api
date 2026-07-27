package com.kara.kara_general_api.infrastructure.adapter.output.security

import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.HashingAlgorithm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SamstevensTotpAdapterTest {
    private val sut = SamstevensTotpAdapter()
    private val referenceGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1)

    @Test
    fun `should generate a base32 secret usable by authenticator apps`() {
        val secret = sut.generateSecret()

        assertTrue(Regex("^[A-Z2-7]+$").matches(secret), "secret base32 attendu, reçu : $secret")
    }

    @Test
    fun `should generate a different secret on every call`() {
        assertNotEquals(sut.generateSecret(), sut.generateSecret())
    }

    @Test
    fun `should build an otpauth uri carrying the Kara issuer and the interoperable profile`() {
        val uri = sut.otpauthUri("JBSWY3DPEHPK3PXP", "client@kara.app")

        assertTrue(uri.startsWith("otpauth://totp/"), uri)
        assertTrue(uri.contains("issuer=Kara"), uri)
        assertTrue(uri.contains("algorithm=SHA1"), uri)
        assertTrue(uri.contains("digits=6"), uri)
        assertTrue(uri.contains("period=30"), uri)
    }

    @Test
    fun `should accept the code of the current time step`() {
        val secret = sut.generateSecret()
        val code = referenceGenerator.generate(secret, sut.currentStep())

        assertTrue(sut.verify(secret, code))
    }

    @Test
    fun `should accept the code of the previous time step within the tolerance window`() {
        val secret = sut.generateSecret()
        val code = referenceGenerator.generate(secret, sut.currentStep() - 1)

        assertTrue(sut.verify(secret, code))
    }

    @Test
    fun `should reject a code well outside the tolerance window`() {
        val secret = sut.generateSecret()
        val code = referenceGenerator.generate(secret, sut.currentStep() - 50)

        assertFalse(sut.verify(secret, code))
    }

    @Test
    fun `should reject a code produced by another secret`() {
        val code = referenceGenerator.generate(sut.generateSecret(), sut.currentStep())

        assertFalse(sut.verify(sut.generateSecret(), code))
    }

    @Test
    fun `should tolerate surrounding whitespace in the submitted code`() {
        val secret = sut.generateSecret()
        val code = referenceGenerator.generate(secret, sut.currentStep())

        assertTrue(sut.verify(secret, "  $code "))
    }

    @Test
    fun `should expose the current time step as a 30-second window`() {
        val step = sut.currentStep()

        assertEquals(System.currentTimeMillis() / 1000 / 30, step)
    }
}

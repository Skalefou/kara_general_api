package com.kara.kara_general_api.domain.model.user.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwoFactorSecretTest {
    private val userId = UserId(UUID.randomUUID())
    private val now = Instant.parse("2026-07-26T10:00:00Z")

    @Test
    fun `should be pending and inactive when freshly created`() {
        val secret = TwoFactorSecret.pending(userId, "cipher", now)

        assertEquals(TwoFactorStatus.PENDING, secret.status)
        assertFalse(secret.isActive)
        assertNull(secret.activatedAt)
        assertNull(secret.lastUsedStep)
    }

    @Test
    fun `should become active and consume the confirming step when activated`() {
        val secret = TwoFactorSecret.pending(userId, "cipher", now).activate(now, step = 1_000L)

        assertTrue(secret.isActive)
        assertEquals(now, secret.activatedAt)
        assertEquals(1_000L, secret.lastUsedStep)
    }

    @Test
    fun `should consider a step unused when no step was ever consumed`() {
        val secret = TwoFactorSecret.pending(userId, "cipher", now)

        assertFalse(secret.isStepAlreadyUsed(1_000L))
    }

    @Test
    fun `should consider the last consumed step as already used`() {
        val secret = TwoFactorSecret.pending(userId, "cipher", now).withLastUsedStep(1_000L)

        assertTrue(secret.isStepAlreadyUsed(1_000L))
    }

    @Test
    fun `should refuse any step older than the last consumed one`() {
        val secret = TwoFactorSecret.pending(userId, "cipher", now).withLastUsedStep(1_000L)

        assertTrue(secret.isStepAlreadyUsed(999L))
    }

    @Test
    fun `should accept a step newer than the last consumed one`() {
        val secret = TwoFactorSecret.pending(userId, "cipher", now).withLastUsedStep(1_000L)

        assertFalse(secret.isStepAlreadyUsed(1_001L))
    }
}

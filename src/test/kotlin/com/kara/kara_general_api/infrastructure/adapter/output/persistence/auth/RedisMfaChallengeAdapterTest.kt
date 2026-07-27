package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedisMfaChallengeAdapterTest {
    private val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    private val valueOperations = mockk<ValueOperations<String, String>>(relaxed = true)
    private val sut = RedisMfaChallengeAdapter(redisTemplate)

    private val userId = UserId(UUID.randomUUID())

    init {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    @Test
    fun `should store the userId under the mfa-challenge key with the requested ttl when issuing`() {
        val keySlot = slot<String>()
        val ttlSlot = slot<Duration>()
        every { valueOperations.set(capture(keySlot), userId.value.toString(), capture(ttlSlot)) } returns Unit

        val token = sut.issue(userId, Duration.ofMinutes(5))

        assertTrue(keySlot.captured.startsWith("mfa-challenge:"))
        assertEquals(keySlot.captured.removePrefix("mfa-challenge:"), token)
        assertEquals(Duration.ofMinutes(5), ttlSlot.captured)
    }

    @Test
    fun `should generate an unpredictable token on every issue`() {
        assertTrue(sut.issue(userId, Duration.ofMinutes(5)) != sut.issue(userId, Duration.ofMinutes(5)))
    }

    @Test
    fun `should return the userId when the challenge exists`() {
        every { valueOperations.get("mfa-challenge:some-token") } returns userId.value.toString()

        assertEquals(userId, sut.findUserId("some-token"))
    }

    @Test
    fun `should return null when the challenge is unknown or expired`() {
        every { valueOperations.get("mfa-challenge:unknown-token") } returns null

        assertNull(sut.findUserId("unknown-token"))
    }

    @Test
    fun `should increment the attempts counter and refresh its ttl`() {
        every { valueOperations.increment("mfa-attempts:some-token") } returns 3L
        val ttlSlot = slot<Duration>()
        every { redisTemplate.expire("mfa-attempts:some-token", capture(ttlSlot)) } returns true

        assertEquals(3, sut.incrementAttempts("some-token"))
        assertEquals(Duration.ofMinutes(5), ttlSlot.captured)
    }

    @Test
    fun `should delete both the challenge and its attempts counter`() {
        sut.delete("some-token")

        verify { redisTemplate.delete("mfa-challenge:some-token") }
        verify { redisTemplate.delete("mfa-attempts:some-token") }
    }
}

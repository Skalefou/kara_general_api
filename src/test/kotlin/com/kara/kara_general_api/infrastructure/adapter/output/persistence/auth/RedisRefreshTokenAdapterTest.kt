package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedisRefreshTokenAdapterTest {

    private val redisTemplate = mockk<StringRedisTemplate>()
    private val valueOperations = mockk<ValueOperations<String, String>>()
    private val sut = RedisRefreshTokenAdapter(redisTemplate)

    private val userId = UserId(UUID.randomUUID())

    init {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    @Test
    fun `should store userId under refresh-token key with 7-day ttl when issuing`() {
        val keySlot = slot<String>()
        val ttlSlot = slot<Duration>()
        every { valueOperations.set(capture(keySlot), userId.value.toString(), capture(ttlSlot)) } returns Unit

        val result = sut.issue(userId)

        assertTrue(keySlot.captured.startsWith("refresh-token:"))
        assertEquals(keySlot.captured.removePrefix("refresh-token:"), result.value)
        assertEquals(Duration.ofDays(7), ttlSlot.captured)
        assertEquals(Duration.ofDays(7).toSeconds(), result.expiresInSeconds)
    }

    @Test
    fun `should return userId and delete key when redeeming a valid token`() {
        every { valueOperations.get("refresh-token:some-token") } returns userId.value.toString()
        every { redisTemplate.delete("refresh-token:some-token") } returns true

        val result = sut.redeem("some-token")

        assertEquals(userId.value, result)
        verify { redisTemplate.delete("refresh-token:some-token") }
    }

    @Test
    fun `should return null when redeeming an unknown token`() {
        every { valueOperations.get("refresh-token:unknown-token") } returns null

        val result = sut.redeem("unknown-token")

        assertNull(result)
        verify(exactly = 0) { redisTemplate.delete(any<String>()) }
    }

    @Test
    fun `should delete key when revoking a token`() {
        every { redisTemplate.delete("refresh-token:some-token") } returns true

        sut.revoke("some-token")

        verify { redisTemplate.delete("refresh-token:some-token") }
    }
}

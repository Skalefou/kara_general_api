package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedisRefreshTokenAdapterTest {
    private val redisTemplate = mockk<StringRedisTemplate>(relaxed = true)
    private val valueOperations = mockk<ValueOperations<String, String>>()
    private val setOperations = mockk<SetOperations<String, String>>(relaxed = true)
    private val sut = RedisRefreshTokenAdapter(redisTemplate)

    private val userId = UserId(UUID.randomUUID())

    init {
        every { redisTemplate.opsForValue() } returns valueOperations
        every { redisTemplate.opsForSet() } returns setOperations
    }

    @Test
    fun `should store userId under refresh-token key with 7-day ttl and index it by user when issuing`() {
        val keySlot = slot<String>()
        val ttlSlot = slot<Duration>()
        every { valueOperations.set(capture(keySlot), userId.value.toString(), capture(ttlSlot)) } returns Unit

        val result = sut.issue(userId)

        assertTrue(keySlot.captured.startsWith("refresh-token:"))
        assertEquals(keySlot.captured.removePrefix("refresh-token:"), result.value)
        assertEquals(Duration.ofDays(7), ttlSlot.captured)
        assertEquals(Duration.ofDays(7).toSeconds(), result.expiresInSeconds)
        verify { setOperations.add("refresh-tokens:user:${userId.value}", result.value) }
    }

    @Test
    fun `should return userId and delete key when redeeming a valid token`() {
        every { valueOperations.get("refresh-token:some-token") } returns userId.value.toString()

        val result = sut.redeem("some-token")

        assertEquals(userId.value, result)
        verify { redisTemplate.delete("refresh-token:some-token") }
        verify { setOperations.remove("refresh-tokens:user:${userId.value}", "some-token") }
    }

    @Test
    fun `should return null when redeeming an unknown token`() {
        every { valueOperations.get("refresh-token:unknown-token") } returns null

        val result = sut.redeem("unknown-token")

        assertNull(result)
        verify(exactly = 0) { redisTemplate.delete(any<String>()) }
    }

    @Test
    fun `should delete key and drop it from the user index when revoking a token`() {
        every { valueOperations.get("refresh-token:some-token") } returns userId.value.toString()

        sut.revoke("some-token")

        verify { redisTemplate.delete("refresh-token:some-token") }
        verify { setOperations.remove("refresh-tokens:user:${userId.value}", "some-token") }
    }

    @Test
    fun `should delete every token and the index when revoking all tokens of a user`() {
        val setKey = "refresh-tokens:user:${userId.value}"
        every { setOperations.members(setKey) } returns mutableSetOf("tok1", "tok2")

        sut.revokeAllForUser(userId)

        verify { redisTemplate.delete("refresh-token:tok1") }
        verify { redisTemplate.delete("refresh-token:tok2") }
        verify { redisTemplate.delete(setKey) }
    }
}

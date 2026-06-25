package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.vo.Email
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import kotlin.test.assertEquals

class RedisEmailVerificationCodeAdapterTest {

    private val redisTemplate = mockk<StringRedisTemplate>()
    private val valueOperations = mockk<ValueOperations<String, String>>()
    private val sut = RedisEmailVerificationCodeAdapter(redisTemplate)

    private val email = Email("client@kara.app")

    init {
        every { redisTemplate.opsForValue() } returns valueOperations
    }

    @Test
    fun `should store code under email-verification key with ttl`() {
        every { valueOperations.set("email-verification:client@kara.app", "123456", Duration.ofMinutes(10)) } returns Unit

        sut.save(email, "123456", Duration.ofMinutes(10))

        verify { valueOperations.set("email-verification:client@kara.app", "123456", Duration.ofMinutes(10)) }
    }

    @Test
    fun `should return stored code`() {
        every { valueOperations.get("email-verification:client@kara.app") } returns "123456"

        val result = sut.find(email)

        assertEquals("123456", result)
    }

    @Test
    fun `should delete code`() {
        every { redisTemplate.delete("email-verification:client@kara.app") } returns true

        sut.delete(email)

        verify { redisTemplate.delete("email-verification:client@kara.app") }
    }
}

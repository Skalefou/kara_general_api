package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisEmailVerificationCodeAdapter(
    private val redisTemplate: StringRedisTemplate,
) : EmailVerificationCodeRepository {

    override fun save(email: Email, code: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key(email), code, ttl)
    }

    override fun find(email: Email): String? = redisTemplate.opsForValue().get(key(email))

    override fun delete(email: Email) {
        redisTemplate.delete(key(email))
    }

    private fun key(email: Email): String = "email-verification:${email.value}"
}

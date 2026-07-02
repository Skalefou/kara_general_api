package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.UUID

private val REFRESH_TOKEN_TTL: Duration = Duration.ofDays(7)
private const val TOKEN_BYTE_LENGTH = 32

@Component
class RedisRefreshTokenAdapter(
    private val redisTemplate: StringRedisTemplate,
) : RefreshTokenRepository {

    private val secureRandom = SecureRandom()

    override fun issue(userId: UserId): RefreshToken {
        val value = generateTokenValue()
        redisTemplate.opsForValue().set(key(value), userId.value.toString(), REFRESH_TOKEN_TTL)
        return RefreshToken(value = value, expiresInSeconds = REFRESH_TOKEN_TTL.toSeconds())
    }

    override fun redeem(token: String): UUID? {
        val storedUserId = redisTemplate.opsForValue().get(key(token)) ?: return null
        redisTemplate.delete(key(token))
        return UUID.fromString(storedUserId)
    }

    override fun revoke(token: String) {
        redisTemplate.delete(key(token))
    }

    private fun generateTokenValue(): String {
        val bytes = ByteArray(TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun key(token: String): String = "refresh-token:$token"
}

package com.kara.kara_general_api.infrastructure.adapter.output.persistence.auth

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.MfaChallengeRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.UUID

private const val TOKEN_BYTE_LENGTH = 32

/**
 * Challenges A2F stockés en Redis, avec expiration native :
 * - `mfa-challenge:<token>` → UUID de l'utilisateur ;
 * - `mfa-attempts:<token>`  → compteur de tentatives infructueuses.
 *
 * Les deux clés portent le même TTL que le challenge : rien ne survit à son expiration.
 */
@Component
class RedisMfaChallengeAdapter(
    private val redisTemplate: StringRedisTemplate,
) : MfaChallengeRepository {
    private val secureRandom = SecureRandom()

    override fun issue(
        userId: UserId,
        ttl: Duration,
    ): String {
        val token = generateTokenValue()
        redisTemplate.opsForValue().set(challengeKey(token), userId.value.toString(), ttl)
        return token
    }

    override fun findUserId(token: String): UserId? =
        redisTemplate
            .opsForValue()
            .get(challengeKey(token))
            ?.let { UserId(UUID.fromString(it)) }

    override fun incrementAttempts(token: String): Int {
        val key = attemptsKey(token)
        val attempts = redisTemplate.opsForValue().increment(key) ?: 0L
        // Le TTL est (re)posé à chaque incrément : le compteur ne survit jamais au challenge.
        redisTemplate.expire(key, CHALLENGE_TTL)
        return attempts.toInt()
    }

    override fun delete(token: String) {
        redisTemplate.delete(challengeKey(token))
        redisTemplate.delete(attemptsKey(token))
    }

    private fun generateTokenValue(): String {
        val bytes = ByteArray(TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun challengeKey(token: String): String = "mfa-challenge:$token"

    private fun attemptsKey(token: String): String = "mfa-attempts:$token"

    private companion object {
        val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
    }
}

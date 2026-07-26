package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.TokenService
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPrivateKey
import java.time.Duration
import java.time.Instant
import java.util.Date

private val ACCESS_TOKEN_TTL: Duration = Duration.ofMinutes(15)

@Component
class JwtTokenAdapter(
    private val jwtPrivateKey: RSAPrivateKey,
) : TokenService {
    override fun generateAccessToken(user: User): AccessToken {
        val now = Instant.now()
        val expiresAt = now.plus(ACCESS_TOKEN_TTL)

        val token =
            Jwts
                .builder()
                .subject(user.id.value.toString())
                .claim("email", user.email.value)
                .claim("role", user.role.name)
                .claim("emailVerified", user.emailVerified)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtPrivateKey, Jwts.SIG.RS256)
                .compact()

        return AccessToken(value = token, expiresInSeconds = ACCESS_TOKEN_TTL.toSeconds())
    }
}

package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtTokenAdapterTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val privateKey = keyPair.private as RSAPrivateKey
    private val publicKey = keyPair.public as RSAPublicKey
    private val sut = JwtTokenAdapter(privateKey)

    private val user =
        User(
            id = UserId(UUID.randomUUID()),
            email = Email("client@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1995, 5, 20),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = true,
        )

    @Test
    fun `should generate a RS256 token with user claims and a 15 minute expiry`() {
        val accessToken = sut.generateAccessToken(user)

        val claims =
            Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(accessToken.value)
                .payload

        assertEquals(user.id.value.toString(), claims.subject)
        assertEquals(user.email.value, claims["email"])
        assertEquals(user.role.name, claims["role"])
        assertEquals(true, claims["emailVerified"])
        assertEquals(900L, accessToken.expiresInSeconds)
        assertTrue(claims.expiration.after(claims.issuedAt))
    }
}

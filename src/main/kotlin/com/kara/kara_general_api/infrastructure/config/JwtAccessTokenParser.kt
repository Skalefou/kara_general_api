package com.kara.kara_general_api.infrastructure.config

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPublicKey

/**
 * Point unique de vérification des JWT d'accès (RS256). Partagé par le filtre HTTP et l'intercepteur
 * STOMP afin qu'aucune manipulation de clé ne soit dupliquée.
 */
@Component
class JwtAccessTokenParser(
    private val jwtPublicKey: RSAPublicKey,
) {
    data class AuthenticatedUser(val userId: String, val role: String)

    /** Vérifie la signature et l'expiration ; renvoie null si le jeton est invalide. */
    fun parse(token: String): AuthenticatedUser? =
        try {
            val claims =
                Jwts.parser()
                    .verifyWith(jwtPublicKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            AuthenticatedUser(userId = claims.subject, role = claims["role"] as? String ?: "")
        } catch (_: JwtException) {
            null
        }
}

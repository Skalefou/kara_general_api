package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.port.output.PasswordGenerator
import org.springframework.stereotype.Component
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

private const val STAFF_PASSWORD_LENGTH = 32
private const val CLIENT_PASSWORD_LENGTH = 16

// Jeux de caractères sans ambiguïté visuelle (pas de 0/O/1/l/I).
private const val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
private const val LOWERCASE = "abcdefghijkmnpqrstuvwxyz"
private const val DIGITS = "23456789"
private const val SPECIALS = "!*&\$@#%?"

@Component
class SecureRandomPasswordGenerator : PasswordGenerator {
    private val random = SecureRandom().asKotlinRandom()

    override fun generate(role: UserRole): String {
        val length =
            when (role) {
                UserRole.SERVER, UserRole.ADMIN -> STAFF_PASSWORD_LENGTH
                UserRole.CLIENT, UserRole.GUEST -> CLIENT_PASSWORD_LENGTH
            }
        val all = UPPERCASE + LOWERCASE + DIGITS + SPECIALS
        // Garantit au moins un caractère de chaque catégorie, puis complète aléatoirement.
        val chars =
            mutableListOf(
                UPPERCASE.random(random),
                LOWERCASE.random(random),
                DIGITS.random(random),
                SPECIALS.random(random),
            )
        repeat(length - chars.size) { chars += all.random(random) }
        chars.shuffle(random)
        return chars.joinToString("")
    }
}

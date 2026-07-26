package com.kara.kara_general_api.domain.port.output

/**
 * Chiffrement symétrique réversible des secrets applicatifs (secret TOTP). Nécessairement réversible :
 * la vérification d'un code TOTP exige le secret en clair, un haché ne suffirait pas.
 */
interface SecretCipher {
    fun encrypt(plain: String): String

    fun decrypt(cipherText: String): String
}

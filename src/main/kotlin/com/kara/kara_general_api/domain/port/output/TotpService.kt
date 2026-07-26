package com.kara.kara_general_api.domain.port.output

/**
 * Primitives TOTP (RFC 6238) : SHA-1, 6 chiffres, période de 30 s — le profil compatible avec Google
 * Authenticator, Proton Authenticator et Aegis.
 */
interface TotpService {
    /** Génère un nouveau secret partagé, encodé en base32 (forme attendue par les applications OTP). */
    fun generateSecret(): String

    /** URI `otpauth://totp/...` à encoder dans le QR code. [accountName] identifie le compte (email). */
    fun otpauthUri(
        secret: String,
        accountName: String,
    ): String

    /** Vrai si [code] est valide pour [secret] dans la fenêtre de tolérance. */
    fun verify(
        secret: String,
        code: String,
    ): Boolean

    /** Pas de temps courant (epoch seconds / 30) — support de l'anti-rejeu. */
    fun currentStep(): Long
}

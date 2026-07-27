package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.port.output.TotpService
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.springframework.stereotype.Component

/** Émetteur affiché par l'application d'authentification (Google Authenticator, Proton, Aegis…). */
private const val ISSUER = "Kara"

/** Profil TOTP interopérable : SHA-1, 6 chiffres, période de 30 s (RFC 6238 + de facto standard OTP). */
private val HASHING_ALGORITHM = HashingAlgorithm.SHA1
private const val CODE_DIGITS = 6
private const val TIME_PERIOD_SECONDS = 30

/** Tolérance de ±1 pas de temps, pour absorber une horloge de téléphone légèrement décalée. */
private const val ALLOWED_TIME_PERIOD_DISCREPANCY = 1

/**
 * Longueur du secret partagé, en **caractères base32**. La librairie en dérive `32 × 5 / 8` = 20 octets
 * aléatoires, soit 160 bits (taille de bloc SHA-1) et un encodage sans caractère de remplissage `=`.
 */
private const val SECRET_BASE32_LENGTH = 32

@Component
class SamstevensTotpAdapter : TotpService {
    private val secretGenerator = DefaultSecretGenerator(SECRET_BASE32_LENGTH)
    private val timeProvider = SystemTimeProvider()
    private val codeVerifier =
        DefaultCodeVerifier(DefaultCodeGenerator(HASHING_ALGORITHM), timeProvider).apply {
            setAllowedTimePeriodDiscrepancy(ALLOWED_TIME_PERIOD_DISCREPANCY)
            setTimePeriod(TIME_PERIOD_SECONDS)
        }

    override fun generateSecret(): String = secretGenerator.generate()

    override fun otpauthUri(
        secret: String,
        accountName: String,
    ): String =
        QrData
            .Builder()
            // Le label `Kara:<email>` est ce que l'application OTP affiche dans sa liste de comptes.
            .label("$ISSUER:$accountName")
            .secret(secret)
            .issuer(ISSUER)
            .algorithm(HASHING_ALGORITHM)
            .digits(CODE_DIGITS)
            .period(TIME_PERIOD_SECONDS)
            .build()
            .uri

    override fun verify(
        secret: String,
        code: String,
    ): Boolean = codeVerifier.isValidCode(secret, code.trim())

    override fun currentStep(): Long = timeProvider.time / TIME_PERIOD_SECONDS
}

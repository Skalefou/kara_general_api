package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken

data class ConsumeRecoveryCodeCommand(
    val mfaToken: String,
    val recoveryCode: String,
)

sealed interface ConsumeRecoveryCodeResult {
    /** L'A2F est retombée désactivée : le compte doit être reconfiguré depuis zéro s'il la veut à nouveau. */
    data class Success(
        val user: User,
        val accessToken: AccessToken,
        val refreshToken: RefreshToken,
        val mustChangePassword: Boolean,
    ) : ConsumeRecoveryCodeResult

    data object ChallengeExpired : ConsumeRecoveryCodeResult

    /** Code inconnu **ou** déjà consommé : réponse volontairement indifférenciée (pas d'oracle). */
    data object InvalidRecoveryCode : ConsumeRecoveryCodeResult

    data object TooManyAttempts : ConsumeRecoveryCodeResult
}

/**
 * Seconde étape de la connexion par code de secours : consomme le code, **désactive l'A2F** (secret et
 * codes restants invalidés), notifie par email et délivre les tokens.
 */
interface ConsumeRecoveryCodeUseCase {
    fun consume(command: ConsumeRecoveryCodeCommand): ConsumeRecoveryCodeResult
}

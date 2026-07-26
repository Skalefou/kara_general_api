package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken

data class VerifyTwoFactorChallengeCommand(
    val mfaToken: String,
    val code: String,
)

sealed interface VerifyTwoFactorChallengeResult {
    data class Success(
        val user: User,
        val accessToken: AccessToken,
        val refreshToken: RefreshToken,
        val mustChangePassword: Boolean,
    ) : VerifyTwoFactorChallengeResult

    /** Challenge inconnu, expiré ou déjà consommé : la connexion doit repartir du mot de passe. */
    data object ChallengeExpired : VerifyTwoFactorChallengeResult

    data object InvalidCode : VerifyTwoFactorChallengeResult

    /** Plafond de tentatives atteint : le challenge est détruit. */
    data object TooManyAttempts : VerifyTwoFactorChallengeResult
}

/** Seconde étape de la connexion : valide le code TOTP et délivre les tokens. */
interface VerifyTwoFactorChallengeUseCase {
    fun verify(command: VerifyTwoFactorChallengeCommand): VerifyTwoFactorChallengeResult
}

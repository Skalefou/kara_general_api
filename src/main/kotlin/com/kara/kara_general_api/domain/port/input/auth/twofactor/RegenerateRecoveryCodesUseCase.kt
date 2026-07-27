package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId

data class RegenerateRecoveryCodesCommand(
    val userId: UserId,
    val password: String,
    /** Code TOTP courant uniquement : un code de secours ne peut pas servir à renouveler la série. */
    val code: String,
)

sealed interface RegenerateRecoveryCodesResult {
    /** [recoveryCodes] en clair : seule et unique occasion de les afficher. */
    data class Success(
        val recoveryCodes: List<String>,
    ) : RegenerateRecoveryCodesResult

    data object UserNotFound : RegenerateRecoveryCodesResult

    data object InvalidPassword : RegenerateRecoveryCodesResult

    data object NotEnabled : RegenerateRecoveryCodesResult

    data object InvalidCode : RegenerateRecoveryCodesResult
}

/** Régénère la série complète de codes de secours ; tous les anciens codes sont invalidés. */
interface RegenerateRecoveryCodesUseCase {
    fun regenerate(command: RegenerateRecoveryCodesCommand): RegenerateRecoveryCodesResult
}

package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId

data class ActivateTwoFactorCommand(
    val userId: UserId,
    val code: String,
)

sealed interface ActivateTwoFactorResult {
    /** [recoveryCodes] en clair : seule et unique occasion de les afficher. */
    data class Success(
        val recoveryCodes: List<String>,
    ) : ActivateTwoFactorResult

    data object UserNotFound : ActivateTwoFactorResult

    data object SetupNotFound : ActivateTwoFactorResult

    data object AlreadyEnabled : ActivateTwoFactorResult

    data object InvalidCode : ActivateTwoFactorResult
}

/** Confirme l'activation de l'A2F par un premier code TOTP valide et délivre les codes de secours. */
interface ActivateTwoFactorUseCase {
    fun activate(command: ActivateTwoFactorCommand): ActivateTwoFactorResult
}

package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId

data class DisableTwoFactorCommand(
    val userId: UserId,
    val password: String,
    /** Code TOTP courant **ou** code de secours non consommé. */
    val code: String,
)

sealed interface DisableTwoFactorResult {
    data object Success : DisableTwoFactorResult

    data object UserNotFound : DisableTwoFactorResult

    data object InvalidPassword : DisableTwoFactorResult

    data object NotEnabled : DisableTwoFactorResult

    data object InvalidCode : DisableTwoFactorResult
}

/**
 * Désactive l'A2F : invalide le secret et l'intégralité des codes de secours restants, puis notifie
 * l'utilisateur par email. Exige mot de passe **et** second facteur.
 */
interface DisableTwoFactorUseCase {
    fun disable(command: DisableTwoFactorCommand): DisableTwoFactorResult
}

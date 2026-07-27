package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId

data class SetupTwoFactorCommand(
    val userId: UserId,
    val password: String,
)

sealed interface SetupTwoFactorResult {
    /**
     * [secret] est la clé partagée **en clair** : elle est affichée une seule fois (QR code + saisie
     * manuelle) et n'est jamais persistée ni loguée sous cette forme.
     */
    data class Success(
        val secret: String,
        val otpauthUri: String,
    ) : SetupTwoFactorResult

    data object UserNotFound : SetupTwoFactorResult

    data object InvalidPassword : SetupTwoFactorResult

    data object AlreadyEnabled : SetupTwoFactorResult
}

/** Prépare l'activation de l'A2F : génère un secret TOTP en attente de confirmation. */
interface SetupTwoFactorUseCase {
    fun setup(command: SetupTwoFactorCommand): SetupTwoFactorResult
}

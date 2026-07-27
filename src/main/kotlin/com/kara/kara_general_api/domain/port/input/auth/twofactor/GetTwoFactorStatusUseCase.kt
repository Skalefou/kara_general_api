package com.kara.kara_general_api.domain.port.input.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId

data class GetTwoFactorStatusCommand(
    val userId: UserId,
)

sealed interface GetTwoFactorStatusResult {
    data class Success(
        val enabled: Boolean,
        val pendingSetup: Boolean,
        val remainingRecoveryCodes: Int,
    ) : GetTwoFactorStatusResult

    data object UserNotFound : GetTwoFactorStatusResult
}

/** Expose l'état A2F du compte (l'entité `User` n'en porte rien : le statut se lit ici). */
interface GetTwoFactorStatusUseCase {
    fun status(command: GetTwoFactorStatusCommand): GetTwoFactorStatusResult
}

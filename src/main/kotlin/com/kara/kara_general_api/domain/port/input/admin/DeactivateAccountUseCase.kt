package com.kara.kara_general_api.domain.port.input.admin

import com.kara.kara_general_api.domain.model.user.UserId

data class DeactivateAccountCommand(
    val userId: UserId,
)

sealed interface DeactivateAccountResult {
    data object Success : DeactivateAccountResult

    data object UserNotFound : DeactivateAccountResult

    data object AlreadyDeactivated : DeactivateAccountResult
}

interface DeactivateAccountUseCase {
    fun deactivate(command: DeactivateAccountCommand): DeactivateAccountResult
}

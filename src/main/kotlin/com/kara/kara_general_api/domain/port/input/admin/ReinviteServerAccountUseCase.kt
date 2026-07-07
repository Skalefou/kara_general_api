package com.kara.kara_general_api.domain.port.input.admin

import com.kara.kara_general_api.domain.model.user.UserId

data class ReinviteServerAccountCommand(
    val userId: UserId,
)

sealed interface ReinviteServerAccountResult {
    data object Success : ReinviteServerAccountResult

    data object UserNotFound : ReinviteServerAccountResult

    data object NotAServer : ReinviteServerAccountResult
}

interface ReinviteServerAccountUseCase {
    fun reinvite(command: ReinviteServerAccountCommand): ReinviteServerAccountResult
}

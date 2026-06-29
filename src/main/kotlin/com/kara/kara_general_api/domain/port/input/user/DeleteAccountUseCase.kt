package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.UserId

data class DeleteAccountCommand(
    val userId: UserId,
    val password: String,
)

sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data object UserNotFound : DeleteAccountResult
    data object InvalidPassword : DeleteAccountResult
}

interface DeleteAccountUseCase {
    fun deleteAccount(command: DeleteAccountCommand): DeleteAccountResult
}

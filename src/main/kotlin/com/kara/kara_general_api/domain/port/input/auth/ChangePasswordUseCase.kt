package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.UserId

data class ChangePasswordCommand(
    val userId: UserId,
    val currentPassword: String,
    val newPassword: String,
)

sealed interface ChangePasswordResult {
    data object Success : ChangePasswordResult

    data object UserNotFound : ChangePasswordResult

    data object InvalidCurrentPassword : ChangePasswordResult

    data class WeakPassword(
        val reasons: List<String>,
    ) : ChangePasswordResult
}

interface ChangePasswordUseCase {
    fun changePassword(command: ChangePasswordCommand): ChangePasswordResult
}

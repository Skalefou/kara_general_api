package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.vo.Email

data class ResetPasswordCommand(
    val email: Email,
    val code: String,
    val newPassword: String,
)

sealed interface ResetPasswordResult {
    data object Success : ResetPasswordResult

    data object UserNotFound : ResetPasswordResult

    data object CodeExpiredOrMissing : ResetPasswordResult

    data object InvalidCode : ResetPasswordResult

    data class InvalidPassword(
        val reasons: List<String>,
    ) : ResetPasswordResult
}

interface ResetPasswordUseCase {
    fun resetPassword(command: ResetPasswordCommand): ResetPasswordResult
}

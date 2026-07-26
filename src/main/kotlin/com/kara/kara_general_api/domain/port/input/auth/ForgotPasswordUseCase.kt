package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.vo.Email

data class ForgotPasswordCommand(
    val email: Email,
)

sealed interface ForgotPasswordResult {
    data object Success : ForgotPasswordResult
}

interface ForgotPasswordUseCase {
    fun requestReset(command: ForgotPasswordCommand): ForgotPasswordResult
}

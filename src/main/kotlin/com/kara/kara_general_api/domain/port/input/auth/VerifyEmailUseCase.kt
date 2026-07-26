package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken

interface VerifyEmailUseCase {
    fun verify(command: VerifyEmailCommand): VerifyEmailResult
}

data class VerifyEmailCommand(
    val email: Email,
    val code: String,
)

sealed interface VerifyEmailResult {
    data class Success(
        val accessToken: AccessToken,
        val refreshToken: RefreshToken,
    ) : VerifyEmailResult

    data object UserNotFound : VerifyEmailResult

    data object AlreadyVerified : VerifyEmailResult

    data object CodeExpiredOrMissing : VerifyEmailResult

    data object InvalidCode : VerifyEmailResult
}

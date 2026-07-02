package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken

data class RefreshTokenCommand(
    val refreshToken: String,
)

sealed interface RefreshTokenResult {
    data class Success(val accessToken: AccessToken, val refreshToken: RefreshToken) : RefreshTokenResult

    data object InvalidToken : RefreshTokenResult
}

interface RefreshTokenUseCase {
    fun refresh(command: RefreshTokenCommand): RefreshTokenResult
}

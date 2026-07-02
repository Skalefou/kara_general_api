package com.kara.kara_general_api.domain.port.input.auth

data class LogoutCommand(
    val refreshToken: String,
)

interface LogoutUseCase {
    fun logout(command: LogoutCommand)
}

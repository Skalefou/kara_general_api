package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.LogoutCommand
import com.kara.kara_general_api.domain.port.input.auth.LogoutUseCase
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import org.springframework.stereotype.Service

@Service
class LogoutService(
    private val refreshTokenRepository: RefreshTokenRepository,
) : LogoutUseCase {

    override fun logout(command: LogoutCommand) {
        refreshTokenRepository.revoke(command.refreshToken)
    }
}

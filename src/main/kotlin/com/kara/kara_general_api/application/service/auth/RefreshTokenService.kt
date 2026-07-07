package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenCommand
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenResult
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenUseCase
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

@Service
class RefreshTokenService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenService: TokenService,
) : RefreshTokenUseCase {

    override fun refresh(command: RefreshTokenCommand): RefreshTokenResult {
        val userId = refreshTokenRepository.redeem(command.refreshToken) ?: return RefreshTokenResult.InvalidToken
        val user = userRepository.findById(UserId(userId)) ?: return RefreshTokenResult.InvalidToken

        if (user.deactivatedAt != null) {
            return RefreshTokenResult.InvalidToken
        }

        val accessToken = tokenService.generateAccessToken(user)
        val refreshToken = refreshTokenRepository.issue(user.id)
        return RefreshTokenResult.Success(accessToken, refreshToken)
    }
}

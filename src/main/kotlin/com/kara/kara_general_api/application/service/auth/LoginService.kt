package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.LoginCommand
import com.kara.kara_general_api.domain.port.input.auth.LoginIdentifier
import com.kara.kara_general_api.domain.port.input.auth.LoginResult
import com.kara.kara_general_api.domain.port.input.auth.LoginUseCase
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

@Service
class LoginService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
) : LoginUseCase {

    override fun login(command: LoginCommand): LoginResult {
        val user =
            when (val identifier = command.identifier) {
                is LoginIdentifier.ByEmail -> userRepository.findByEmail(identifier.email)
                is LoginIdentifier.ByPhoneNumber -> userRepository.findByPhoneNumber(identifier.phoneNumber)
            } ?: return LoginResult.UserNotFound

        if (user.deletedAt != null) {
            return LoginResult.AccountDeleted
        }

        if (!passwordHasher.matches(command.password, user.hashedPassword)) {
            return LoginResult.InvalidCredentials
        }

        val accessToken = tokenService.generateAccessToken(user)
        val refreshToken = refreshTokenRepository.issue(user.id)
        return LoginResult.Success(user, accessToken, refreshToken)
    }
}

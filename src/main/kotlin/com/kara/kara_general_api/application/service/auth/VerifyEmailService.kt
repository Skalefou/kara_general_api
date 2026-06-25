package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailCommand
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailResult
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailUseCase
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

@Service
class VerifyEmailService(
    private val userRepository: UserRepository,
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val tokenService: TokenService,
) : VerifyEmailUseCase {

    override fun verify(command: VerifyEmailCommand): VerifyEmailResult {
        val user = userRepository.findByEmail(command.email) ?: return VerifyEmailResult.UserNotFound

        if (user.emailVerified) {
            return VerifyEmailResult.AlreadyVerified
        }

        val storedCode = emailVerificationCodeRepository.find(command.email) ?: return VerifyEmailResult.CodeExpiredOrMissing

        if (storedCode != command.code) {
            return VerifyEmailResult.InvalidCode
        }

        userRepository.markEmailVerified(user.id)
        emailVerificationCodeRepository.delete(command.email)

        val accessToken = tokenService.generateAccessToken(user.verifyEmail())
        return VerifyEmailResult.Success(accessToken)
    }
}

package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordUseCase
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordResetCodeRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration

private val PASSWORD_RESET_CODE_TTL: Duration = Duration.ofMinutes(15)

@Service
class ForgotPasswordService(
    private val userRepository: UserRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val emailService: EmailService,
) : ForgotPasswordUseCase {

    override fun requestReset(command: ForgotPasswordCommand): ForgotPasswordResult {
        val user = userRepository.findByEmail(command.email)
            ?: return ForgotPasswordResult.Success

        val code = (100000..999999).random().toString()
        passwordResetCodeRepository.save(user.email, code, PASSWORD_RESET_CODE_TTL)
        emailService.sendPasswordResetCode(user.email, code)

        return ForgotPasswordResult.Success
    }
}

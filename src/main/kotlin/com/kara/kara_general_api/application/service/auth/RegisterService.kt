package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.PasswordPolicy
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.port.input.auth.RegisterCommand
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.input.auth.RegisterUseCase
import com.kara.kara_general_api.domain.port.output.EmailAlreadyUsedException
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration

private val EMAIL_VERIFICATION_CODE_TTL: Duration = Duration.ofMinutes(10)

@Service
class RegisterService(
    private val userRepository: UserRepository,
    private val firebaseAuthGateway: FirebaseAuthGateway,
    private val passwordHasher: PasswordHasher,
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val emailService: EmailService,
) : RegisterUseCase {
    override fun register(command: RegisterCommand): RegisterResult {
        if (userRepository.existsByEmail(command.email)) {
            return RegisterResult.EmailAlreadyUsed
        }

        val passwordIssues = PasswordPolicy.validate(command.plainPassword, UserRole.CLIENT)
        if (passwordIssues.isNotEmpty()) {
            return RegisterResult.InvalidPassword(passwordIssues)
        }

        val firebaseUserId =
            try {
                firebaseAuthGateway.createUser(command.email, command.plainPassword)
            } catch (_: EmailAlreadyUsedException) {
                return RegisterResult.EmailAlreadyUsed
            }

        val user =
            try {
                val user =
                    User.register(
                        email = command.email,
                        hashedPassword = passwordHasher.hash(command.plainPassword),
                        firstName = command.firstName,
                        lastName = command.lastName,
                        phoneNumber = command.phoneNumber,
                        birthDate = command.birthDate,
                        firebaseUid = firebaseUserId.value,
                    )
                userRepository.save(user)
            } catch (e: Exception) {
                firebaseAuthGateway.deleteUser(firebaseUserId)
                throw e
            }

        val verificationCode = (100000..999999).random().toString()
        emailVerificationCodeRepository.save(user.email, verificationCode, EMAIL_VERIFICATION_CODE_TTL)
        emailService.sendVerificationCode(user.email, verificationCode)

        return RegisterResult.Success(user)
    }
}

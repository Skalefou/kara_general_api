package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.port.input.user.UpdateProfileCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileUseCase
import com.kara.kara_general_api.domain.port.output.EmailAlreadyUsedException
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

private val EMAIL_VERIFICATION_CODE_TTL: Duration = Duration.ofMinutes(10)

@Service
class UpdateProfileService(
    private val userRepository: UserRepository,
    private val firebaseAuthGateway: FirebaseAuthGateway,
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val emailService: EmailService,
) : UpdateProfileUseCase {

    @Transactional
    override fun updateProfile(command: UpdateProfileCommand): UpdateProfileResult {
        val user = userRepository.findById(command.userId) ?: return UpdateProfileResult.UserNotFound

        var updated =
            user.updateProfile(
                firstName = command.firstName,
                lastName = command.lastName,
                phoneNumber = command.phoneNumber,
                birthDate = command.birthDate,
            )

        val newEmail = command.email
        val emailChanged = newEmail != null && newEmail != user.email

        if (emailChanged) {
            if (userRepository.existsByEmail(newEmail)) {
                return UpdateProfileResult.EmailAlreadyUsed
            }
            try {
                firebaseAuthGateway.updateEmail(FirebaseUserId(user.firebaseUid), newEmail)
            } catch (_: EmailAlreadyUsedException) {
                return UpdateProfileResult.EmailAlreadyUsed
            }
            updated = updated.changeEmail(newEmail)
        }

        try {
            userRepository.update(updated)
        } catch (e: Exception) {
            if (emailChanged) {
                // Compensation : le rollback DB est automatique (@Transactional), mais Firebase est externe.
                firebaseAuthGateway.updateEmail(FirebaseUserId(user.firebaseUid), user.email)
            }
            throw e
        }

        if (emailChanged) {
            val verificationCode = (100000..999999).random().toString()
            emailVerificationCodeRepository.save(newEmail, verificationCode, EMAIL_VERIFICATION_CODE_TTL)
            emailService.sendVerificationCode(newEmail, verificationCode)
        }

        return UpdateProfileResult.Success(updated)
    }
}

package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordUseCase
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.PasswordResetCodeRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ResetPasswordService(
    private val userRepository: UserRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val passwordHasher: PasswordHasher,
    private val firebaseAuthGateway: FirebaseAuthGateway,
) : ResetPasswordUseCase {

    @Transactional
    override fun resetPassword(command: ResetPasswordCommand): ResetPasswordResult {
        val user = userRepository.findByEmail(command.email)
            ?: return ResetPasswordResult.UserNotFound

        val storedCode = passwordResetCodeRepository.find(command.email)
            ?: return ResetPasswordResult.CodeExpiredOrMissing

        if (storedCode != command.code) {
            return ResetPasswordResult.InvalidCode
        }

        val passwordIssues = validatePassword(command.newPassword)
        if (passwordIssues.isNotEmpty()) {
            return ResetPasswordResult.InvalidPassword(passwordIssues)
        }

        userRepository.updatePassword(user.id, passwordHasher.hash(command.newPassword))
        firebaseAuthGateway.updatePassword(FirebaseUserId(user.firebaseUid), command.newPassword)
        passwordResetCodeRepository.delete(command.email)

        return ResetPasswordResult.Success
    }

    private fun validatePassword(password: String): List<String> {
        val issues = mutableListOf<String>()
        if (password.length < 8) issues += "Le mot de passe doit contenir au moins 8 caractères"
        if (!password.any { it.isDigit() }) issues += "Le mot de passe doit contenir au moins un chiffre"
        if (!password.any { it.isLetter() }) issues += "Le mot de passe doit contenir au moins une lettre"
        return issues
    }
}

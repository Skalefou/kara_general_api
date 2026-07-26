package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.PasswordPolicy
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordUseCase
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangePasswordService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val firebaseAuthGateway: FirebaseAuthGateway,
) : ChangePasswordUseCase {
    @Transactional
    override fun changePassword(command: ChangePasswordCommand): ChangePasswordResult {
        val user = userRepository.findById(command.userId) ?: return ChangePasswordResult.UserNotFound

        if (!passwordHasher.matches(command.currentPassword, user.hashedPassword)) {
            return ChangePasswordResult.InvalidCurrentPassword
        }

        val passwordIssues = PasswordPolicy.validate(command.newPassword, user.role)
        if (passwordIssues.isNotEmpty()) {
            return ChangePasswordResult.WeakPassword(passwordIssues)
        }

        firebaseAuthGateway.updatePassword(FirebaseUserId(user.firebaseUid), command.newPassword)
        userRepository.updatePassword(user.id, passwordHasher.hash(command.newPassword))

        return ChangePasswordResult.Success
    }
}

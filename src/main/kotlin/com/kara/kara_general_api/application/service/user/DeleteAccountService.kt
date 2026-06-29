package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.port.input.user.DeleteAccountCommand
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountUseCase
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteAccountService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val firebaseAuthGateway: FirebaseAuthGateway,
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val emailService: EmailService,
) : DeleteAccountUseCase {

    @Transactional
    override fun deleteAccount(command: DeleteAccountCommand): DeleteAccountResult {
        val user = userRepository.findById(command.userId)
            ?: return DeleteAccountResult.UserNotFound

        if (!passwordHasher.matches(command.password, user.hashedPassword)) {
            return DeleteAccountResult.InvalidPassword
        }

        emailService.sendAccountDeletionConfirmation(user.email)
        firebaseAuthGateway.deleteUser(FirebaseUserId(user.firebaseUid))
        emailVerificationCodeRepository.delete(user.email)
        userRepository.anonymize(user.id)

        return DeleteAccountResult.Success
    }
}

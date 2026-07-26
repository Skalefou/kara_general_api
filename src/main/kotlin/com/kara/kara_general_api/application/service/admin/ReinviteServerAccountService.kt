package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountResult
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountUseCase
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordGenerator
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private val TEMP_PASSWORD_TTL: Duration = Duration.ofHours(24)

@Service
class ReinviteServerAccountService(
    private val userRepository: UserRepository,
    private val firebaseAuthGateway: FirebaseAuthGateway,
    private val passwordHasher: PasswordHasher,
    private val passwordGenerator: PasswordGenerator,
    private val emailService: EmailService,
) : ReinviteServerAccountUseCase {
    @Transactional
    override fun reinvite(command: ReinviteServerAccountCommand): ReinviteServerAccountResult {
        val user = userRepository.findById(command.userId) ?: return ReinviteServerAccountResult.UserNotFound
        if (user.role != UserRole.SERVER) {
            return ReinviteServerAccountResult.NotAServer
        }

        val temporaryPassword = passwordGenerator.generate(UserRole.SERVER)
        val expiresAt = Instant.now().plus(TEMP_PASSWORD_TTL)

        firebaseAuthGateway.updatePassword(FirebaseUserId(user.firebaseUid), temporaryPassword)
        userRepository.applyReinvitation(user.id, passwordHasher.hash(temporaryPassword), expiresAt)

        emailService.sendServerInvitation(user.email, user.firstName, temporaryPassword, expiresAt)

        return ReinviteServerAccountResult.Success
    }
}

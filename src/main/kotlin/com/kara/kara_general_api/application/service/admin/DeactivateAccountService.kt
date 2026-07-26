package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountResult
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountUseCase
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeactivateAccountService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) : DeactivateAccountUseCase {
    @Transactional
    override fun deactivate(command: DeactivateAccountCommand): DeactivateAccountResult {
        val user = userRepository.findById(command.userId) ?: return DeactivateAccountResult.UserNotFound
        if (user.deactivatedAt != null) {
            return DeactivateAccountResult.AlreadyDeactivated
        }

        userRepository.update(user.deactivate())
        // Coupe immédiatement les sessions actives (les access tokens courants expirent sous ≤15 min).
        refreshTokenRepository.revokeAllForUser(command.userId)

        return DeactivateAccountResult.Success
    }
}

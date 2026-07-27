package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusUseCase
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TwoFactorStatusService(
    private val userRepository: UserRepository,
    private val twoFactorRepository: TwoFactorRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
) : GetTwoFactorStatusUseCase {
    @Transactional(readOnly = true)
    override fun status(command: GetTwoFactorStatusCommand): GetTwoFactorStatusResult {
        val user = userRepository.findById(command.userId) ?: return GetTwoFactorStatusResult.UserNotFound
        val secret = twoFactorRepository.findByUserId(user.id)

        val enabled = secret?.isActive == true
        return GetTwoFactorStatusResult.Success(
            enabled = enabled,
            // Un secret existe mais n'a jamais été confirmé : le front peut reprendre l'activation.
            pendingSetup = secret != null && !secret.isActive,
            // Tant que l'A2F n'est pas active, aucun code de secours n'a été délivré.
            remainingRecoveryCodes = if (enabled) recoveryCodeRepository.countUnused(user.id) else 0,
        )
    }
}

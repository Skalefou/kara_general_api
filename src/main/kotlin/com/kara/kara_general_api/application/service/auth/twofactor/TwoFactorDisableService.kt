package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeNormalizer
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorUseCase
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Désactivation de l'A2F. Exige deux preuves : le mot de passe **et** un second facteur (code TOTP courant
 * ou code de secours encore disponible). Le secret et l'intégralité des codes restants sont supprimés, puis
 * l'utilisateur est notifié par email.
 */
@Service
class TwoFactorDisableService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val twoFactorRepository: TwoFactorRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val totpService: TotpService,
    private val secretCipher: SecretCipher,
    private val emailService: EmailService,
) : DisableTwoFactorUseCase {
    @Transactional
    override fun disable(command: DisableTwoFactorCommand): DisableTwoFactorResult {
        val user = userRepository.findById(command.userId) ?: return DisableTwoFactorResult.UserNotFound

        if (!passwordHasher.matches(command.password, user.hashedPassword)) {
            return DisableTwoFactorResult.InvalidPassword
        }

        val secret = twoFactorRepository.findByUserId(user.id)
        if (secret == null || !secret.isActive) {
            return DisableTwoFactorResult.NotEnabled
        }

        val totpValid = totpService.verify(secretCipher.decrypt(secret.secretCipher), command.code)
        if (!totpValid && !matchesUnusedRecoveryCode(user.id, command.code)) {
            return DisableTwoFactorResult.InvalidCode
        }

        twoFactorRepository.deleteByUserId(user.id)
        recoveryCodeRepository.deleteByUserId(user.id)

        emailService.sendTwoFactorDisabled(user.email)

        return DisableTwoFactorResult.Success
    }

    private fun matchesUnusedRecoveryCode(
        userId: UserId,
        rawCode: String,
    ): Boolean {
        val normalized = RecoveryCodeNormalizer.normalize(rawCode)
        return recoveryCodeRepository.findUnusedByUserId(userId).any {
            passwordHasher.matches(normalized, HashedPassword(it.codeHash))
        }
    }
}

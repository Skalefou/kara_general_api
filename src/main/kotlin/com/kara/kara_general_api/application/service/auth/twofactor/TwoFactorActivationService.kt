package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeNormalizer
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorUseCase
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeGenerator
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Seconde étape de l'activation : un premier code TOTP valide prouve que l'application d'authentification
 * est correctement configurée. L'A2F passe alors `ACTIVE` et les codes de secours sont délivrés — c'est la
 * seule et unique fois où ils sont visibles en clair.
 */
@Service
class TwoFactorActivationService(
    private val userRepository: UserRepository,
    private val twoFactorRepository: TwoFactorRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val recoveryCodeGenerator: RecoveryCodeGenerator,
    private val totpService: TotpService,
    private val secretCipher: SecretCipher,
    private val passwordHasher: PasswordHasher,
    private val emailService: EmailService,
) : ActivateTwoFactorUseCase {
    @Transactional
    override fun activate(command: ActivateTwoFactorCommand): ActivateTwoFactorResult {
        val user = userRepository.findById(command.userId) ?: return ActivateTwoFactorResult.UserNotFound
        val secret =
            twoFactorRepository.findByUserId(user.id) ?: return ActivateTwoFactorResult.SetupNotFound

        if (secret.isActive) {
            return ActivateTwoFactorResult.AlreadyEnabled
        }

        if (!totpService.verify(secretCipher.decrypt(secret.secretCipher), command.code)) {
            return ActivateTwoFactorResult.InvalidCode
        }

        // Le pas du code qui vient de servir est consommé d'emblée : il ne pourra pas être rejoué.
        twoFactorRepository.save(secret.activate(Instant.now(), totpService.currentStep()))

        val plainCodes =
            recoveryCodeGenerator.generate(
                count = TwoFactorPolicy.RECOVERY_CODE_COUNT,
                wordsPerCode = TwoFactorPolicy.RECOVERY_CODE_WORDS,
            )
        recoveryCodeRepository.replaceAll(user.id, plainCodes.map(::hashOf))

        emailService.sendTwoFactorEnabled(user.email)

        return ActivateTwoFactorResult.Success(plainCodes)
    }

    private fun hashOf(plainCode: String): String =
        passwordHasher.hash(RecoveryCodeNormalizer.normalize(plainCode)).value
}

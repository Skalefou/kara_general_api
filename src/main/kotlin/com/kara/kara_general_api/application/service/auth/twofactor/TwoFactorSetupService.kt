package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorUseCase
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Première étape de l'activation : génère un secret TOTP et le range en `PENDING`. Le secret en clair est
 * retourné à l'appelant (QR code + saisie manuelle) puis oublié ; seule sa forme chiffrée est persistée.
 */
@Service
class TwoFactorSetupService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val twoFactorRepository: TwoFactorRepository,
    private val totpService: TotpService,
    private val secretCipher: SecretCipher,
) : SetupTwoFactorUseCase {
    @Transactional
    override fun setup(command: SetupTwoFactorCommand): SetupTwoFactorResult {
        val user = userRepository.findById(command.userId) ?: return SetupTwoFactorResult.UserNotFound

        if (!passwordHasher.matches(command.password, user.hashedPassword)) {
            return SetupTwoFactorResult.InvalidPassword
        }

        // Une A2F active ne se reconfigure pas : il faut d'abord la désactiver (mot de passe + code).
        if (twoFactorRepository.findByUserId(user.id)?.isActive == true) {
            return SetupTwoFactorResult.AlreadyEnabled
        }

        val secret = totpService.generateSecret()
        // Écrase un éventuel PENDING précédent : un nouveau QR code invalide l'ancien.
        twoFactorRepository.save(
            TwoFactorSecret.pending(
                userId = user.id,
                secretCipher = secretCipher.encrypt(secret),
                now = Instant.now(),
            ),
        )

        return SetupTwoFactorResult.Success(
            secret = secret,
            otpauthUri = totpService.otpauthUri(secret, user.email.value),
        )
    }
}

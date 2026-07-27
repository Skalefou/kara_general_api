package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeNormalizer
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesUseCase
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeGenerator
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Renouvelle la série de codes de secours. Exige le mot de passe **et** un code TOTP valide (un code de
 * secours ne suffit pas : il servirait à se refabriquer une série depuis un code volé). Tous les anciens
 * codes sont invalidés.
 */
@Service
class RegenerateRecoveryCodesService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val twoFactorRepository: TwoFactorRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val recoveryCodeGenerator: RecoveryCodeGenerator,
    private val totpService: TotpService,
    private val secretCipher: SecretCipher,
) : RegenerateRecoveryCodesUseCase {
    @Transactional
    override fun regenerate(command: RegenerateRecoveryCodesCommand): RegenerateRecoveryCodesResult {
        val user =
            userRepository.findById(command.userId) ?: return RegenerateRecoveryCodesResult.UserNotFound

        if (!passwordHasher.matches(command.password, user.hashedPassword)) {
            return RegenerateRecoveryCodesResult.InvalidPassword
        }

        val secret = twoFactorRepository.findByUserId(user.id)
        if (secret == null || !secret.isActive) {
            return RegenerateRecoveryCodesResult.NotEnabled
        }

        if (!totpService.verify(secretCipher.decrypt(secret.secretCipher), command.code)) {
            return RegenerateRecoveryCodesResult.InvalidCode
        }

        val plainCodes =
            recoveryCodeGenerator.generate(
                count = TwoFactorPolicy.RECOVERY_CODE_COUNT,
                wordsPerCode = TwoFactorPolicy.RECOVERY_CODE_WORDS,
            )
        recoveryCodeRepository.replaceAll(
            user.id,
            plainCodes.map { passwordHasher.hash(RecoveryCodeNormalizer.normalize(it)).value },
        )

        return RegenerateRecoveryCodesResult.Success(plainCodes)
    }
}

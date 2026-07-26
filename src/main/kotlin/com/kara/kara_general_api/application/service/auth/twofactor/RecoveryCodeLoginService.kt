package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeNormalizer
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ConsumeRecoveryCodeCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ConsumeRecoveryCodeResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ConsumeRecoveryCodeUseCase
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.MfaChallengeRepository
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Connexion de secours : l'utilisateur a perdu son application d'authentification et présente un code de
 * secours. Le code est consommé, **puis l'A2F est intégralement démontée** (secret + codes restants) —
 * l'utilisateur retrouve un compte à simple mot de passe et devra reconfigurer l'A2F s'il la souhaite.
 */
@Service
class RecoveryCodeLoginService(
    private val mfaChallengeRepository: MfaChallengeRepository,
    private val userRepository: UserRepository,
    private val twoFactorRepository: TwoFactorRepository,
    private val recoveryCodeRepository: RecoveryCodeRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailService: EmailService,
) : ConsumeRecoveryCodeUseCase {
    @Transactional
    override fun consume(command: ConsumeRecoveryCodeCommand): ConsumeRecoveryCodeResult {
        val userId =
            mfaChallengeRepository.findUserId(command.mfaToken)
                ?: return ConsumeRecoveryCodeResult.ChallengeExpired

        val user = userRepository.findById(userId)
        if (user == null) {
            mfaChallengeRepository.delete(command.mfaToken)
            return ConsumeRecoveryCodeResult.ChallengeExpired
        }

        val normalized = RecoveryCodeNormalizer.normalize(command.recoveryCode)
        // Un code inconnu et un code déjà consommé sont indiscernables : `findUnusedByUserId` exclut déjà
        // les codes utilisés, donc les deux cas retombent sur le même échec.
        val match =
            recoveryCodeRepository.findUnusedByUserId(userId).firstOrNull {
                passwordHasher.matches(normalized, HashedPassword(it.codeHash))
            } ?: return registerFailure(command.mfaToken)

        recoveryCodeRepository.markUsed(match.id)

        twoFactorRepository.deleteByUserId(userId)
        recoveryCodeRepository.deleteByUserId(userId)

        emailService.sendTwoFactorDisabled(user.email)

        mfaChallengeRepository.delete(command.mfaToken)

        return ConsumeRecoveryCodeResult.Success(
            user = user,
            accessToken = tokenService.generateAccessToken(user),
            refreshToken = refreshTokenRepository.issue(user.id),
            mustChangePassword = user.mustChangePassword,
        )
    }

    private fun registerFailure(mfaToken: String): ConsumeRecoveryCodeResult {
        val attempts = mfaChallengeRepository.incrementAttempts(mfaToken)
        if (attempts >= TwoFactorPolicy.MAX_CHALLENGE_ATTEMPTS) {
            mfaChallengeRepository.delete(mfaToken)
            return ConsumeRecoveryCodeResult.TooManyAttempts
        }
        return ConsumeRecoveryCodeResult.InvalidRecoveryCode
    }
}

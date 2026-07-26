package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.port.input.auth.twofactor.VerifyTwoFactorChallengeCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.VerifyTwoFactorChallengeResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.VerifyTwoFactorChallengeUseCase
import com.kara.kara_general_api.domain.port.output.MfaChallengeRepository
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Seconde étape de la connexion A2F : consomme le challenge émis par `LoginService` et délivre les tokens.
 *
 * Trois garde-fous :
 * - le challenge est à usage unique (détruit dès qu'il aboutit) ;
 * - anti-rejeu : un pas de temps TOTP déjà consommé est refusé, même si le code est mathématiquement valide ;
 * - plafond de tentatives : au-delà de [TwoFactorPolicy.MAX_CHALLENGE_ATTEMPTS] échecs, le challenge est
 *   détruit et la connexion doit repartir du mot de passe.
 */
@Service
class TwoFactorChallengeService(
    private val mfaChallengeRepository: MfaChallengeRepository,
    private val userRepository: UserRepository,
    private val twoFactorRepository: TwoFactorRepository,
    private val totpService: TotpService,
    private val secretCipher: SecretCipher,
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
) : VerifyTwoFactorChallengeUseCase {
    @Transactional
    override fun verify(command: VerifyTwoFactorChallengeCommand): VerifyTwoFactorChallengeResult {
        val userId =
            mfaChallengeRepository.findUserId(command.mfaToken)
                ?: return VerifyTwoFactorChallengeResult.ChallengeExpired

        val user = userRepository.findById(userId)
        val secret = twoFactorRepository.findByUserId(userId)
        if (user == null || secret == null || !secret.isActive) {
            // Le compte ou son A2F a disparu entre les deux étapes : le challenge n'a plus de sens.
            mfaChallengeRepository.delete(command.mfaToken)
            return VerifyTwoFactorChallengeResult.ChallengeExpired
        }

        if (!totpService.verify(secretCipher.decrypt(secret.secretCipher), command.code)) {
            return registerFailure(command.mfaToken)
        }

        val step = totpService.currentStep()
        if (secret.isStepAlreadyUsed(step)) {
            // Rejeu d'un code déjà servi : traité exactement comme un code invalide (pas d'oracle).
            return registerFailure(command.mfaToken)
        }
        twoFactorRepository.updateLastUsedStep(userId, step)

        mfaChallengeRepository.delete(command.mfaToken)

        return VerifyTwoFactorChallengeResult.Success(
            user = user,
            accessToken = tokenService.generateAccessToken(user),
            refreshToken = refreshTokenRepository.issue(user.id),
            mustChangePassword = user.mustChangePassword,
        )
    }

    private fun registerFailure(mfaToken: String): VerifyTwoFactorChallengeResult {
        val attempts = mfaChallengeRepository.incrementAttempts(mfaToken)
        if (attempts >= TwoFactorPolicy.MAX_CHALLENGE_ATTEMPTS) {
            mfaChallengeRepository.delete(mfaToken)
            return VerifyTwoFactorChallengeResult.TooManyAttempts
        }
        return VerifyTwoFactorChallengeResult.InvalidCode
    }
}

package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.LoginCommand
import com.kara.kara_general_api.domain.port.input.auth.LoginIdentifier
import com.kara.kara_general_api.domain.port.input.auth.LoginResult
import com.kara.kara_general_api.domain.port.input.auth.LoginUseCase
import com.kara.kara_general_api.domain.port.output.MfaChallengeRepository
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/** Durée de vie du challenge A2F : au-delà, la connexion repart de la saisie du mot de passe. */
private val MFA_CHALLENGE_TTL: Duration = Duration.ofMinutes(5)

@Service
class LoginService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val twoFactorRepository: TwoFactorRepository,
    private val mfaChallengeRepository: MfaChallengeRepository,
) : LoginUseCase {
    override fun login(command: LoginCommand): LoginResult {
        val user =
            when (val identifier = command.identifier) {
                is LoginIdentifier.ByEmail -> userRepository.findByEmail(identifier.email)
                is LoginIdentifier.ByPhoneNumber -> userRepository.findByPhoneNumber(identifier.phoneNumber)
            } ?: return LoginResult.UserNotFound

        if (user.deletedAt != null) {
            return LoginResult.AccountDeleted
        }

        if (user.deactivatedAt != null) {
            return LoginResult.AccountDeactivated
        }

        if (!passwordHasher.matches(command.password, user.hashedPassword)) {
            return LoginResult.InvalidCredentials
        }

        if (user.isTempPasswordExpired(Instant.now())) {
            return LoginResult.TempPasswordExpired
        }

        // Second facteur exigé : on s'arrête ici et on n'émet AUCUN token. Le challenge, à lui seul, ne donne
        // accès à rien — il ne sert qu'à corréler la seconde étape.
        if (twoFactorRepository.findByUserId(user.id)?.isActive == true) {
            val mfaToken = mfaChallengeRepository.issue(user.id, MFA_CHALLENGE_TTL)
            return LoginResult.TwoFactorRequired(
                mfaToken = mfaToken,
                expiresInSeconds = MFA_CHALLENGE_TTL.toSeconds(),
            )
        }

        val accessToken = tokenService.generateAccessToken(user)
        val refreshToken = refreshTokenRepository.issue(user.id)
        return LoginResult.Success(user, accessToken, refreshToken, mustChangePassword = user.mustChangePassword)
    }
}

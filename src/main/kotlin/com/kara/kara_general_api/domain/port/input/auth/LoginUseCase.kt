package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken

sealed interface LoginIdentifier {
    data class ByEmail(
        val email: Email,
    ) : LoginIdentifier

    data class ByPhoneNumber(
        val phoneNumber: PhoneNumber,
    ) : LoginIdentifier
}

data class LoginCommand(
    val identifier: LoginIdentifier,
    val password: String,
)

sealed interface LoginResult {
    data class Success(
        val user: User,
        val accessToken: AccessToken,
        val refreshToken: RefreshToken,
        val mustChangePassword: Boolean,
    ) : LoginResult

    /**
     * Mot de passe validé, mais le compte exige un second facteur : aucun token n'est délivré. Le front doit
     * rejouer [mfaToken] sur `/api/v1/auth/login/2fa` (code TOTP) ou `/api/v1/auth/login/2fa/recovery`
     * (code de secours) avant [expiresInSeconds].
     */
    data class TwoFactorRequired(
        val mfaToken: String,
        val expiresInSeconds: Long,
    ) : LoginResult

    data object UserNotFound : LoginResult

    data object InvalidCredentials : LoginResult

    data object AccountDeleted : LoginResult

    data object AccountDeactivated : LoginResult

    data object TempPasswordExpired : LoginResult
}

interface LoginUseCase {
    fun login(command: LoginCommand): LoginResult
}

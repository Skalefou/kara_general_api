package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordUseCase
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordUseCase
import com.kara.kara_general_api.domain.port.input.auth.LoginCommand
import com.kara.kara_general_api.domain.port.input.auth.LoginIdentifier
import com.kara.kara_general_api.domain.port.input.auth.LoginResult
import com.kara.kara_general_api.domain.port.input.auth.LoginUseCase
import com.kara.kara_general_api.domain.port.input.auth.LogoutCommand
import com.kara.kara_general_api.domain.port.input.auth.LogoutUseCase
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenCommand
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenResult
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenUseCase
import com.kara.kara_general_api.domain.port.input.auth.RegisterCommand
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.input.auth.RegisterUseCase
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordUseCase
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailCommand
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailResult
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ChangePasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ForgotPasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LoginRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LoginResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LogoutRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RefreshTokenRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RefreshTokenResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ResetPasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
) : AuthApi {

    override fun register(request: RegisterRequest): ResponseEntity<Any> {
        val command =
            RegisterCommand(
                email = Email(request.email),
                plainPassword = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                phoneNumber = PhoneNumber(request.phoneNumber),
                birthDate = request.birthDate,
            )

        return when (val result = registerUseCase.register(command)) {
            is RegisterResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(
                    RegisterResponse(
                        id = result.user.id.value,
                        email = result.user.email.value,
                        firstName = result.user.firstName,
                        lastName = result.user.lastName,
                    ),
                )

            RegisterResult.EmailAlreadyUsed ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Un compte existe déjà avec cet email.",
                    ).apply {
                        title = "Email déjà utilisé"
                        setProperty("code", "EMAIL_ALREADY_USED")
                    },
                )

            is RegisterResult.InvalidPassword ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        result.reasons.joinToString(" "),
                    ).apply {
                        title = "Mot de passe invalide"
                        setProperty("code", "INVALID_PASSWORD")
                    },
                )
        }
    }

    override fun login(request: LoginRequest): ResponseEntity<Any> {
        val identifier =
            if (request.isEmail) {
                LoginIdentifier.ByEmail(Email(request.identifiant))
            } else {
                LoginIdentifier.ByPhoneNumber(PhoneNumber(request.identifiant))
            }
        val command = LoginCommand(identifier = identifier, password = request.password)

        return when (val result = loginUseCase.login(command)) {
            is LoginResult.Success ->
                ResponseEntity.ok(
                    LoginResponse(
                        accessToken = result.accessToken.value,
                        expiresIn = result.accessToken.expiresInSeconds,
                        refreshToken = result.refreshToken.value,
                        refreshTokenExpiresIn = result.refreshToken.expiresInSeconds,
                        user = UserResponse.from(result.user),
                        mustChangePassword = result.mustChangePassword,
                    ),
                )

            LoginResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet identifiant.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )

            LoginResult.InvalidCredentials ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Identifiant ou mot de passe incorrect.",
                    ).apply {
                        title = "Identifiants invalides"
                        setProperty("code", "INVALID_CREDENTIALS")
                    },
                )

            LoginResult.AccountDeleted ->
                ResponseEntity.status(HttpStatus.GONE).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.GONE,
                        "Ce compte a été supprimé.",
                    ).apply {
                        title = "Compte supprimé"
                        setProperty("code", "ACCOUNT_DELETED")
                    },
                )

            LoginResult.AccountDeactivated ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        "Ce compte a été désactivé. Contactez un administrateur.",
                    ).apply {
                        title = "Compte désactivé"
                        setProperty("code", "ACCOUNT_DEACTIVATED")
                    },
                )

            LoginResult.TempPasswordExpired ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        "Le mot de passe temporaire a expiré. Demandez une nouvelle invitation.",
                    ).apply {
                        title = "Mot de passe temporaire expiré"
                        setProperty("code", "TEMP_PASSWORD_EXPIRED")
                    },
                )
        }
    }

    override fun changePassword(request: ChangePasswordRequest, authentication: Authentication): ResponseEntity<Any> {
        val command =
            ChangePasswordCommand(
                userId = UserId(UUID.fromString(authentication.name)),
                currentPassword = request.currentPassword,
                newPassword = request.newPassword,
            )

        return when (val result = changePasswordUseCase.changePassword(command)) {
            ChangePasswordResult.Success ->
                ResponseEntity.noContent().build()

            ChangePasswordResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet identifiant.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )

            ChangePasswordResult.InvalidCurrentPassword ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Le mot de passe actuel est incorrect.",
                    ).apply {
                        title = "Mot de passe incorrect"
                        setProperty("code", "INVALID_CURRENT_PASSWORD")
                    },
                )

            is ChangePasswordResult.WeakPassword ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        result.reasons.joinToString(" "),
                    ).apply {
                        title = "Mot de passe invalide"
                        setProperty("code", "WEAK_PASSWORD")
                    },
                )
        }
    }

    override fun verifyEmail(request: VerifyEmailRequest): ResponseEntity<Any> {
        val command = VerifyEmailCommand(email = Email(request.email), code = request.code)

        return when (val result = verifyEmailUseCase.verify(command)) {
            is VerifyEmailResult.Success ->
                ResponseEntity.ok(
                    VerifyEmailResponse(
                        accessToken = result.accessToken.value,
                        expiresIn = result.accessToken.expiresInSeconds,
                        refreshToken = result.refreshToken.value,
                        refreshTokenExpiresIn = result.refreshToken.expiresInSeconds,
                    ),
                )

            VerifyEmailResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet email.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )

            VerifyEmailResult.AlreadyVerified ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Cet email a déjà été vérifié.",
                    ).apply {
                        title = "Email déjà vérifié"
                        setProperty("code", "EMAIL_ALREADY_VERIFIED")
                    },
                )

            VerifyEmailResult.CodeExpiredOrMissing ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Le code de vérification a expiré. Merci de redemander un nouveau code.",
                    ).apply {
                        title = "Code expiré"
                        setProperty("code", "VERIFICATION_CODE_EXPIRED")
                    },
                )

            VerifyEmailResult.InvalidCode ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Le code de vérification est incorrect.",
                    ).apply {
                        title = "Code invalide"
                        setProperty("code", "INVALID_VERIFICATION_CODE")
                    },
                )
        }
    }

    override fun forgotPassword(request: ForgotPasswordRequest): ResponseEntity<Any> {
        forgotPasswordUseCase.requestReset(ForgotPasswordCommand(email = Email(request.email)))
        return ResponseEntity.noContent().build()
    }

    override fun resetPassword(request: ResetPasswordRequest): ResponseEntity<Any> {
        val command =
            ResetPasswordCommand(
                email = Email(request.email),
                code = request.code,
                newPassword = request.newPassword,
            )

        return when (val result = resetPasswordUseCase.resetPassword(command)) {
            ResetPasswordResult.Success ->
                ResponseEntity.noContent().build()

            ResetPasswordResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet email.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )

            ResetPasswordResult.CodeExpiredOrMissing ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Le code de réinitialisation a expiré. Merci de faire une nouvelle demande.",
                    ).apply {
                        title = "Code expiré"
                        setProperty("code", "RESET_CODE_EXPIRED")
                    },
                )

            ResetPasswordResult.InvalidCode ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Le code de réinitialisation est incorrect.",
                    ).apply {
                        title = "Code invalide"
                        setProperty("code", "INVALID_RESET_CODE")
                    },
                )

            is ResetPasswordResult.InvalidPassword ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        result.reasons.joinToString(" "),
                    ).apply {
                        title = "Mot de passe invalide"
                        setProperty("code", "INVALID_PASSWORD")
                    },
                )
        }
    }

    override fun refresh(request: RefreshTokenRequest): ResponseEntity<Any> {
        val command = RefreshTokenCommand(refreshToken = request.refreshToken)

        return when (val result = refreshTokenUseCase.refresh(command)) {
            is RefreshTokenResult.Success ->
                ResponseEntity.ok(
                    RefreshTokenResponse(
                        accessToken = result.accessToken.value,
                        expiresIn = result.accessToken.expiresInSeconds,
                        refreshToken = result.refreshToken.value,
                        refreshTokenExpiresIn = result.refreshToken.expiresInSeconds,
                    ),
                )

            RefreshTokenResult.InvalidToken ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Le refresh token est invalide, expiré ou a déjà été utilisé.",
                    ).apply {
                        title = "Refresh token invalide"
                        setProperty("code", "INVALID_REFRESH_TOKEN")
                    },
                )
        }
    }

    override fun logout(request: LogoutRequest): ResponseEntity<Any> {
        logoutUseCase.logout(LogoutCommand(refreshToken = request.refreshToken))
        return ResponseEntity.noContent().build()
    }
}

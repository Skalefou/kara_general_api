package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ChangePasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ForgotPasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LoginRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LoginResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LogoutRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RecoveryCodeLoginRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RefreshTokenRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RefreshTokenResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ResetPasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.TwoFactorChallengeResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.TwoFactorLoginRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.common.dto.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Authentification", description = "Opérations liées à l'inscription et à l'authentification des utilisateurs")
interface AuthApi {
    @Operation(
        summary = "Inscrire un nouvel utilisateur",
        description = "Crée un compte utilisateur (PostgreSQL + Firebase) à partir des informations fournies et retourne le profil créé.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Compte créé avec succès",
                content = [Content(schema = Schema(implementation = RegisterResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Mot de passe invalide",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Un compte existe déjà avec cet email",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Se connecter",
        description =
            "Authentifie un utilisateur via email ou téléphone (`isEmail`) et mot de passe. Deux formes de " +
                "réponse 200 : `LoginResponse` (connexion terminée, access token délivré) ou " +
                "`TwoFactorChallengeResponse` (le compte exige un second facteur : aucun token n'est délivré, " +
                "il faut rejouer le `mfaToken` sur /api/v1/auth/login/2fa ou /api/v1/auth/login/2fa/recovery). " +
                "Le front discrimine sur le champ `twoFactorRequired`, présent dans les deux schémas.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                    "Authentification réussie (access token délivré) OU second facteur exigé " +
                        "(`twoFactorRequired: true`)",
                content = [
                    Content(
                        schema =
                            Schema(
                                oneOf = [LoginResponse::class, TwoFactorChallengeResponse::class],
                            ),
                        examples = [
                            ExampleObject(
                                name = "LoginResponse",
                                value = """
                                    {
                                      "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                                      "expiresIn": 900,
                                      "refreshToken": "hR8s...",
                                      "refreshTokenExpiresIn": 604800,
                                      "mustChangePassword": false,
                                      "twoFactorRequired": false,
                                      "twoFactorDisabled": false
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "TwoFactorChallengeResponse",
                                value = """
                                    {
                                      "twoFactorRequired": true,
                                      "mfaToken": "Zm9vYmFyLXRva2Vu...",
                                      "expiresIn": 300
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Identifiant ou mot de passe incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_CREDENTIALS",
                                value = """
                                    {
                                      "title": "Identifiants invalides",
                                      "status": 401,
                                      "detail": "Identifiant ou mot de passe incorrect.",
                                      "instance": "/api/v1/auth/login",
                                      "code": "INVALID_CREDENTIALS"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Aucun compte ne correspond à cet identifiant",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "USER_NOT_FOUND",
                                value = """
                                    {
                                      "title": "Compte introuvable",
                                      "status": 404,
                                      "detail": "Aucun compte ne correspond à cet identifiant.",
                                      "instance": "/api/v1/auth/login",
                                      "code": "USER_NOT_FOUND"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "410",
                description = "Le compte a été supprimé",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "ACCOUNT_DELETED",
                                value = """
                                    {
                                      "title": "Compte supprimé",
                                      "status": 410,
                                      "detail": "Ce compte a été supprimé.",
                                      "instance": "/api/v1/auth/login",
                                      "code": "ACCOUNT_DELETED"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Compte désactivé ou mot de passe temporaire expiré",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "ACCOUNT_DEACTIVATED",
                                value = """
                                    {
                                      "title": "Compte désactivé",
                                      "status": 403,
                                      "detail": "Ce compte a été désactivé. Contactez un administrateur.",
                                      "instance": "/api/v1/auth/login",
                                      "code": "ACCOUNT_DEACTIVATED"
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "TEMP_PASSWORD_EXPIRED",
                                value = """
                                    {
                                      "title": "Mot de passe temporaire expiré",
                                      "status": 403,
                                      "detail": "Le mot de passe temporaire a expiré. Demandez une nouvelle invitation.",
                                      "instance": "/api/v1/auth/login",
                                      "code": "TEMP_PASSWORD_EXPIRED"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Valider le second facteur (code TOTP) et terminer la connexion",
        description =
            "Consomme le `mfaToken` délivré par /api/v1/auth/login et vérifie le code à usage unique. " +
                "Le challenge est à usage unique et un code déjà utilisé est refusé (anti-rejeu).",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Second facteur validé, access token délivré",
                content = [Content(schema = Schema(implementation = LoginResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Code invalide, ou challenge inconnu / expiré / déjà consommé",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_TWO_FACTOR_CODE",
                                value = """
                                    {
                                      "title": "Code invalide",
                                      "status": 400,
                                      "detail": "Le code à usage unique est incorrect ou a déjà été utilisé.",
                                      "instance": "/api/v1/auth/login/2fa",
                                      "code": "INVALID_TWO_FACTOR_CODE"
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "TWO_FACTOR_CHALLENGE_EXPIRED",
                                value = """
                                    {
                                      "title": "Session de vérification expirée",
                                      "status": 400,
                                      "detail": "La session de vérification a expiré. Merci de vous reconnecter.",
                                      "instance": "/api/v1/auth/login/2fa",
                                      "code": "TWO_FACTOR_CHALLENGE_EXPIRED"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "429",
                description = "Trop de tentatives : le challenge a été détruit, il faut se reconnecter",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "TWO_FACTOR_TOO_MANY_ATTEMPTS",
                                value = """
                                    {
                                      "title": "Trop de tentatives",
                                      "status": 429,
                                      "detail": "Trop de codes incorrects ont été saisis. Merci de vous reconnecter.",
                                      "instance": "/api/v1/auth/login/2fa",
                                      "code": "TWO_FACTOR_TOO_MANY_ATTEMPTS"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/login/2fa")
    fun loginTwoFactor(
        @Valid @RequestBody request: TwoFactorLoginRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Se connecter avec un code de secours (perte de l'application d'authentification)",
        description =
            "Consomme le `mfaToken` et un code de secours à usage unique. **Effet de bord majeur** : " +
                "l'A2F du compte est désactivée (clé secrète et codes restants invalidés), la réponse porte " +
                "`twoFactorDisabled: true` et un email de notification est envoyé.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Code de secours accepté, A2F désactivée, access token délivré",
                content = [Content(schema = Schema(implementation = LoginResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Code de secours inconnu ou déjà consommé, ou challenge expiré",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_RECOVERY_CODE",
                                value = """
                                    {
                                      "title": "Code de secours invalide",
                                      "status": 400,
                                      "detail": "Ce code de secours est invalide ou a déjà été utilisé.",
                                      "instance": "/api/v1/auth/login/2fa/recovery",
                                      "code": "INVALID_RECOVERY_CODE"
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "TWO_FACTOR_CHALLENGE_EXPIRED",
                                value = """
                                    {
                                      "title": "Session de vérification expirée",
                                      "status": 400,
                                      "detail": "La session de vérification a expiré. Merci de vous reconnecter.",
                                      "instance": "/api/v1/auth/login/2fa/recovery",
                                      "code": "TWO_FACTOR_CHALLENGE_EXPIRED"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "429",
                description = "Trop de tentatives : le challenge a été détruit, il faut se reconnecter",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "TWO_FACTOR_TOO_MANY_ATTEMPTS",
                                value = """
                                    {
                                      "title": "Trop de tentatives",
                                      "status": 429,
                                      "detail": "Trop de codes incorrects ont été saisis. Merci de vous reconnecter.",
                                      "instance": "/api/v1/auth/login/2fa/recovery",
                                      "code": "TWO_FACTOR_TOO_MANY_ATTEMPTS"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/login/2fa/recovery")
    fun loginWithRecoveryCode(
        @Valid @RequestBody request: RecoveryCodeLoginRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Vérifier l'adresse email",
        description = "Valide le code à 6 chiffres envoyé par email à l'inscription et délivre un access token JWT.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Email vérifié, access token délivré",
                content = [Content(schema = Schema(implementation = VerifyEmailResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Code invalide ou expiré",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Aucun compte ne correspond à cet email",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Email déjà vérifié",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/verify-email")
    fun verifyEmail(
        @Valid @RequestBody request: VerifyEmailRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Demander un code de réinitialisation de mot de passe",
        description = "Envoie un code OTP par email. Retourne toujours 204 même si l'email est inconnu (anti-énumération).",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Code envoyé (ou email silencieusement ignoré)"),
        ],
    )
    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Réinitialiser le mot de passe",
        description = "Valide le code OTP et remplace le mot de passe en base et dans Firebase.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé avec succès"),
            ApiResponse(
                responseCode = "400",
                description = "Code invalide, expiré ou mot de passe trop faible",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Renouveler l'access token",
        description =
            "Échange un refresh token valide contre un nouveau couple access token / refresh token " +
                "(rotation : l'ancien refresh token est invalidé).",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Nouveau couple de tokens délivré",
                content = [Content(schema = Schema(implementation = RefreshTokenResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Refresh token invalide, expiré ou déjà utilisé",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_REFRESH_TOKEN",
                                value = """
                                    {
                                      "title": "Refresh token invalide",
                                      "status": 401,
                                      "detail": "Le refresh token est invalide, expiré ou a déjà été utilisé.",
                                      "instance": "/api/v1/auth/refresh",
                                      "code": "INVALID_REFRESH_TOKEN"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Se déconnecter",
        description =
            "Révoque le refresh token fourni. Retourne toujours 204, y compris si le token est déjà " +
                "invalide ou inconnu (anti-énumération).",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Refresh token révoqué (ou déjà invalide/inconnu, silencieusement ignoré)"),
        ],
    )
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Changer son mot de passe",
        description =
            "Remplace le mot de passe de l'utilisateur authentifié après vérification du mot de passe " +
                "actuel. Utilisé notamment pour le changement forcé à la première connexion d'un compte serveur.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Mot de passe changé avec succès"),
            ApiResponse(
                responseCode = "400",
                description = "Nouveau mot de passe non conforme à la politique",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Mot de passe actuel incorrect",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

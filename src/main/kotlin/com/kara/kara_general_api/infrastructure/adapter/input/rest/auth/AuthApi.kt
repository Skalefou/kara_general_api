package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

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
import com.kara.kara_general_api.infrastructure.adapter.input.rest.common.dto.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
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
        description = "Authentifie un utilisateur via email ou téléphone (`isEmail`) et mot de passe, et délivre un access token JWT.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authentification réussie, access token délivré",
                content = [Content(schema = Schema(implementation = LoginResponse::class))],
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
        ],
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
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
        description = "Échange un refresh token valide contre un nouveau couple access token / refresh token " +
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
        description = "Révoque le refresh token fourni. Retourne toujours 204, y compris si le token est déjà " +
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
}

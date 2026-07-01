package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ForgotPasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.LoginRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.ResetPasswordRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
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
            ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
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
            ApiResponse(responseCode = "200", description = "Authentification réussie, access token délivré"),
            ApiResponse(
                responseCode = "401",
                description = "Identifiant ou mot de passe incorrect",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Aucun compte ne correspond à cet identifiant",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "410",
                description = "Le compte a été supprimé",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
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
            ApiResponse(responseCode = "200", description = "Email vérifié, access token délivré"),
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
}

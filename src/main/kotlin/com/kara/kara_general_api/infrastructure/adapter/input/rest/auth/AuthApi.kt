package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterRequest
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
}

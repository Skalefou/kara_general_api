package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.DeleteAccountRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Utilisateur", description = "Gestion du compte utilisateur")
interface UserApi {

    @Operation(
        summary = "Supprimer son compte",
        description = "Anonymise toutes les données personnelles (RGPD Art. 17). Requiert la confirmation du mot de passe.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Compte supprimé avec succès"),
            ApiResponse(
                responseCode = "401",
                description = "Mot de passe incorrect",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/me")
    fun deleteAccount(
        @Valid @RequestBody request: DeleteAccountRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

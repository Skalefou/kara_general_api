package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.RegisterFcmTokenRequest
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Notifications", description = "Enregistrement des tokens d'appareil pour les notifications push")
interface FcmTokenApi {

    @Operation(
        summary = "Enregistrer son token FCM",
        description = "Enregistre (ou remplace) le token d'appareil FCM du client authentifié, utilisé pour " +
            "lui adresser des notifications push (rappels de fin de réservation notamment).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Token enregistré avec succès"),
            ApiResponse(
                responseCode = "400",
                description = "Token manquant ou vide",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(responseCode = "401", description = "Non authentifié"),
        ],
    )
    @PostMapping("/me/fcm-token")
    fun registerFcmToken(
        @Valid @RequestBody request: RegisterFcmTokenRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

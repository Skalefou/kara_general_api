package com.kara.kara_general_api.infrastructure.adapter.input.rest.order

import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.AvailableProductResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "Commandes", description = "Commande de produits pendant une réservation")
interface BookingMenuApi {
    @Operation(
        summary = "Produits commandables d'une réservation",
        description =
            "Liste les produits en stock (quantité > 0) de la salle de la réservation, que le client " +
                "peut commander. Réservé au client propriétaire de la réservation.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Produits commandables",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AvailableProductResponse::class)))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "La réservation n'appartient pas à l'utilisateur (BOOKING_NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation introuvable (BOOKING_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    fun getAvailableProducts(
        @PathVariable bookingId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.order

import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.OrderResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.PlaceOrderRequest
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Commandes", description = "Commandes de produits pendant une réservation active")
interface OrderApi {

    @Operation(
        summary = "Commander un produit pendant une réservation active",
        description = "Le client commande un produit à consommer pendant sa réservation active (statut " +
            "CONFIRMED et instant courant dans le créneau). Le stock de la salle est décrémenté. Un moyen de " +
            "paiement enregistré est requis : sans lui, aucune commande n'est créée (402) et le client est " +
            "invité à en mettre un en place — le débit automatique est réalisé par la brique paiement, hors " +
            "de ce point d'entrée.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Commande enregistrée",
                content = [Content(schema = Schema(implementation = OrderResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide (VALIDATION_ERROR)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "402",
                description = "Aucun moyen de paiement enregistré (PAYMENT_METHOD_REQUIRED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "La réservation n'appartient pas à l'utilisateur (BOOKING_NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation (BOOKING_NOT_FOUND) ou produit (PRODUCT_NOT_FOUND) introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Réservation non active (BOOKING_NOT_ACTIVE) ou stock insuffisant " +
                    "(INSUFFICIENT_STOCK)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun placeOrder(
        @PathVariable bookingId: UUID,
        @Valid @RequestBody request: PlaceOrderRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

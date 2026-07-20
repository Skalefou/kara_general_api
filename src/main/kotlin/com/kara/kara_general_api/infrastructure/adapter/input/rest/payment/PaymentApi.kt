package com.kara.kara_general_api.infrastructure.adapter.input.rest.payment

import com.kara.kara_general_api.infrastructure.adapter.input.rest.payment.dto.InitiateBookingPaymentResponse
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import java.util.UUID

@Tag(name = "Paiements", description = "Paiement « payer tout » via Stripe (PaymentSheet) et webhook Stripe")
interface PaymentApi {

    @Operation(
        summary = "Initier le paiement d'une réservation",
        description = "Crée (paresseusement) le client Stripe, une clé éphémère et un PaymentIntent pour le montant " +
            "total de la réservation, puis retourne les secrets nécessaires au PaymentSheet. Le paiement n'est " +
            "confirmé que par le webhook Stripe.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Secrets du PaymentSheet retournés",
                content = [Content(schema = Schema(implementation = InitiateBookingPaymentResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "La réservation n'appartient pas à l'utilisateur (PAYMENT_NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation introuvable (PAYMENT_BOOKING_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "La réservation n'est plus en attente de paiement (PAYMENT_ALREADY_PAID)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/api/v1/bookings/{id}/payments")
    fun initiatePayment(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Webhook Stripe",
        description = "Point de terminaison appelé par Stripe. La signature est vérifiée avec STRIPE_WEBHOOK_SECRET. " +
            "Sur payment_intent.succeeded, le paiement passe PAID et la réservation CONFIRMED. Retourne 200 pour tout " +
            "événement traité ou ignoré, 400 si la signature est invalide.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Événement traité ou ignoré"),
            ApiResponse(responseCode = "400", description = "Signature Stripe absente ou invalide"),
        ],
    )
    @PostMapping("/api/v1/stripe/webhook")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader(name = "Stripe-Signature", required = false) signature: String?,
    ): ResponseEntity<Any>
}

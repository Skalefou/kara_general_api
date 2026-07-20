package com.kara.kara_general_api.infrastructure.adapter.input.rest.payment.dto

import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentResult
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * Secrets nécessaires à l'initialisation du PaymentSheet Stripe côté front (Flutter / mobile SDK).
 */
data class InitiateBookingPaymentResponse(
    @field:Schema(description = "Identifiant du paiement (Payment) créé côté Kara")
    val paymentId: UUID,
    @field:Schema(description = "Client secret du PaymentIntent Stripe")
    val clientSecret: String,
    @field:Schema(description = "Secret de la clé éphémère Stripe")
    val ephemeralKeySecret: String,
    @field:Schema(description = "Identifiant client Stripe")
    val customerId: String,
    @field:Schema(description = "Clé publiable Stripe")
    val publishableKey: String,
) {
    companion object {
        fun from(ready: InitiateBookingPaymentResult.Ready): InitiateBookingPaymentResponse =
            InitiateBookingPaymentResponse(
                paymentId = ready.paymentId,
                clientSecret = ready.clientSecret,
                ephemeralKeySecret = ready.ephemeralKeySecret,
                customerId = ready.customerId,
                publishableKey = ready.publishableKey,
            )
    }
}

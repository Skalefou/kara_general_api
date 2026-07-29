package com.kara.kara_general_api.infrastructure.adapter.input.rest.payment.dto

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/** Statuts à jour de la réservation et de son paiement après réconciliation avec Stripe. */
data class SyncBookingPaymentResponse(
    @field:Schema(description = "Identifiant de la réservation")
    val bookingId: UUID,
    @field:Schema(description = "Identifiant du paiement (Payment) côté Kara")
    val paymentId: UUID,
    @field:Schema(description = "Statut de la réservation après réconciliation")
    val bookingStatus: BookingStatus,
    @field:Schema(description = "Statut du paiement après réconciliation")
    val paymentStatus: PaymentStatus,
)

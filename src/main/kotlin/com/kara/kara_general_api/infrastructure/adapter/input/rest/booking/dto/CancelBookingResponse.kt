package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Résumé d'une réservation annulée, avec l'indicateur de remboursement. */
data class CancelBookingResponse(
    val bookingId: UUID,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val totalPrice: BigDecimal,
    val currency: Currency,
    @field:Schema(description = "Nouveau statut (toujours CANCELLED)", example = "CANCELLED")
    val status: BookingStatus,
    val paymentMode: PaymentMode,
    @field:Schema(description = "Vrai si un remboursement Stripe a été émis (réservation qui était confirmée)")
    val refunded: Boolean,
) {
    companion object {
        fun from(
            booking: Booking,
            refunded: Boolean,
        ): CancelBookingResponse =
            CancelBookingResponse(
                bookingId = booking.id.value,
                startAt = booking.startAt,
                endAt = booking.endAt,
                numberOfPeople = booking.numberOfPeople,
                totalPrice = booking.totalPrice,
                currency = booking.currency,
                status = booking.status,
                paymentMode = booking.paymentMode,
                refunded = refunded,
            )
    }
}

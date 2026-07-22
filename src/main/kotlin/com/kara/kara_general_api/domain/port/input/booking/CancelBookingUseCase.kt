package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId

data class CancelBookingCommand(
    val bookingId: BookingId,
    val requesterId: UserId,
)

sealed interface CancelBookingResult {
    /**
     * Réservation annulée. [refunded] vaut vrai si un remboursement Stripe a été émis (réservation qui
     * était CONFIRMED, fonds déjà capturés) ; faux si rien n'a été prélevé (PENDING, ou cagnotte ouverte
     * dont les autorisations ont simplement été levées).
     */
    data class Cancelled(val booking: Booking, val refunded: Boolean) : CancelBookingResult

    data object NotFound : CancelBookingResult

    data object NotOwner : CancelBookingResult

    /** La réservation est déjà annulée (idempotence : un second appel est rejeté). */
    data object AlreadyCancelled : CancelBookingResult

    /** Le début de la réservation est déjà passé : elle ne peut plus être annulée. */
    data object AlreadyStarted : CancelBookingResult
}

interface CancelBookingUseCase {
    fun cancel(command: CancelBookingCommand): CancelBookingResult
}

package com.kara.kara_general_api.domain.port.input.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import java.util.UUID

data class InitiateBookingPaymentCommand(
    val bookingId: BookingId,
    val userId: UserId,
)

sealed interface InitiateBookingPaymentResult {
    /** Secrets destinés au PaymentSheet Stripe côté front. */
    data class Ready(
        val clientSecret: String,
        val ephemeralKeySecret: String,
        val customerId: String,
        val publishableKey: String,
        val paymentId: UUID,
    ) : InitiateBookingPaymentResult

    data object BookingNotFound : InitiateBookingPaymentResult

    data object NotOwner : InitiateBookingPaymentResult

    /** La réservation n'est plus en attente de paiement (déjà confirmée ou annulée). */
    data object AlreadyPaid : InitiateBookingPaymentResult
}

interface InitiateBookingPaymentUseCase {
    fun initiate(command: InitiateBookingPaymentCommand): InitiateBookingPaymentResult
}

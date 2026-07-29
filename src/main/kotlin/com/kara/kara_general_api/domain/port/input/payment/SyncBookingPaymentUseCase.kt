package com.kara.kara_general_api.domain.port.input.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.user.UserId

data class SyncBookingPaymentCommand(
    val bookingId: BookingId,
    val paymentId: PaymentId,
    val userId: UserId,
)

sealed interface SyncBookingPaymentResult {
    /** Statuts à jour après réconciliation (inchangés si l'intent n'est pas encore réglé). */
    data class Synced(
        val bookingStatus: BookingStatus,
        val paymentStatus: PaymentStatus,
    ) : SyncBookingPaymentResult

    data object NotOwner : SyncBookingPaymentResult

    /** Paiement ou réservation introuvable, ou paiement rattaché à une autre réservation. */
    data object NotFound : SyncBookingPaymentResult
}

/**
 * Réconcilie un paiement « payer tout » avec l'état réel de la passerelle. Filet de sécurité appelable par
 * le client quand le webhook Stripe n'est jamais arrivé : sans lui, la réservation reste PENDING puis est
 * annulée par le balayage des réservations expirées alors que le client a bel et bien payé.
 */
interface SyncBookingPaymentUseCase {
    fun sync(command: SyncBookingPaymentCommand): SyncBookingPaymentResult
}

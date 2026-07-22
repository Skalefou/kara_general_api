package com.kara.kara_general_api.domain.model.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant

/**
 * Paiement « payer tout » d'une réservation via Stripe. Le webhook Stripe fait foi pour la transition
 * PENDING → PAID (aucune confirmation optimiste côté API).
 */
data class Payment(
    val id: PaymentId,
    val bookingId: BookingId,
    val userId: UserId,
    val amount: BigDecimal,
    val currency: Currency,
    val status: PaymentStatus,
    val stripePaymentIntentId: String,
    val createdAt: Instant,
) {
    fun markPaid(): Payment = copy(status = PaymentStatus.PAID)

    fun markFailed(): Payment = copy(status = PaymentStatus.FAILED)

    fun markRefunded(): Payment = copy(status = PaymentStatus.REFUNDED)

    companion object {
        /** Paiement initié : PENDING, rattaché au PaymentIntent Stripe déjà créé. */
        fun pending(
            bookingId: BookingId,
            userId: UserId,
            amount: BigDecimal,
            currency: Currency,
            stripePaymentIntentId: String,
        ): Payment =
            Payment(
                id = PaymentId.generate(),
                bookingId = bookingId,
                userId = userId,
                amount = amount,
                currency = currency,
                status = PaymentStatus.PENDING,
                stripePaymentIntentId = stripePaymentIntentId,
                createdAt = Instant.now(),
            )
    }
}

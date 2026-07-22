package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId

interface PaymentRepository {
    /** Persiste (upsert) le paiement. */
    fun save(payment: Payment): Payment

    fun findById(id: PaymentId): Payment?

    /** Retrouve le paiement par l'identifiant de PaymentIntent Stripe (clé utilisée par le webhook). */
    fun findByStripePaymentIntentId(stripePaymentIntentId: String): Payment?

    /** Tous les paiements « payer tout » d'une réservation (0..n : chaque initiation crée une ligne). */
    fun findByBookingId(bookingId: BookingId): List<Payment>
}

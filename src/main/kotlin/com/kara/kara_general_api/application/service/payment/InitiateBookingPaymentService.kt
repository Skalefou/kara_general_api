package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentResult
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Initie un paiement « payer tout » sur une réservation PENDING appartenant au client. Crée
 * paresseusement le client Stripe, la clé éphémère et le PaymentIntent, puis persiste un [Payment]
 * PENDING. Les secrets retournés alimentent le PaymentSheet côté front.
 *
 * Un PaymentIntent déjà ouvert pour cette réservation et encore payable est **réutilisé** (avec sa ligne
 * `payments`) : deux appuis sur « Payer » ne laissent donc pas deux intents payables. Une clé d'idempotence
 * couvre le cas de la création concurrente côté Stripe.
 *
 * La confirmation effective (PAID + réservation CONFIRMED) est faite par le webhook Stripe, ou à défaut par
 * la réconciliation explicite du client (cf. SyncBookingPaymentService).
 */
@Service
class InitiateBookingPaymentService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
) : InitiateBookingPaymentUseCase {
    @Transactional
    override fun initiate(command: InitiateBookingPaymentCommand): InitiateBookingPaymentResult {
        val booking =
            bookingRepository.findById(command.bookingId) ?: return InitiateBookingPaymentResult.BookingNotFound
        if (booking.userId != command.userId) return InitiateBookingPaymentResult.NotOwner
        if (booking.status != BookingStatus.PENDING) return InitiateBookingPaymentResult.AlreadyPaid
        if (!booking.expiresAt.isAfter(Instant.now())) return InitiateBookingPaymentResult.BookingExpired

        val user =
            userRepository.findById(command.userId) ?: return InitiateBookingPaymentResult.BookingNotFound

        val customerId = paymentGateway.ensureCustomer(user)
        if (user.stripeCustomerId != customerId) {
            userRepository.updateStripeCustomerId(user.id, customerId)
        }

        val ephemeralKeySecret = paymentGateway.createEphemeralKey(customerId)

        reusableIntent(booking.id)?.let { (existingPayment, clientSecret) ->
            return ready(clientSecret, ephemeralKeySecret, customerId, existingPayment.id.value)
        }

        val intent =
            paymentGateway.createPaymentIntent(
                amount = booking.totalPrice,
                currency = booking.currency,
                customerId = customerId,
                idempotencyKey = "booking-payment-${booking.id.value}",
            )

        val payment =
            paymentRepository.save(
                Payment.pending(
                    bookingId = booking.id,
                    userId = user.id,
                    amount = booking.totalPrice,
                    currency = booking.currency,
                    stripePaymentIntentId = intent.paymentIntentId,
                ),
            )

        return ready(intent.clientSecret, ephemeralKeySecret, customerId, payment.id.value)
    }

    /**
     * Paiement PENDING déjà ouvert pour cette réservation dont l'intent Stripe est encore payable, avec le
     * client secret à re-servir. Null s'il n'y en a pas (ou si l'intent n'est plus exploitable).
     */
    private fun reusableIntent(bookingId: BookingId): Pair<Payment, String>? {
        val existing = paymentRepository.findPendingByBookingId(bookingId) ?: return null
        val snapshot = paymentGateway.retrievePaymentIntent(existing.stripePaymentIntentId) ?: return null
        if (!snapshot.status.isReusableForPayment()) return null
        val clientSecret = snapshot.clientSecret ?: return null
        return existing to clientSecret
    }

    private fun ready(
        clientSecret: String,
        ephemeralKeySecret: String,
        customerId: String,
        paymentId: UUID,
    ): InitiateBookingPaymentResult.Ready =
        InitiateBookingPaymentResult.Ready(
            clientSecret = clientSecret,
            ephemeralKeySecret = ephemeralKeySecret,
            customerId = customerId,
            publishableKey = paymentGateway.publishableKey(),
            paymentId = paymentId,
        )
}

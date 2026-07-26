package com.kara.kara_general_api.application.service.payment

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

/**
 * Initie un paiement « payer tout » sur une réservation PENDING appartenant au client. Crée
 * paresseusement le client Stripe, la clé éphémère et le PaymentIntent, puis persiste un [Payment]
 * PENDING. Les secrets retournés alimentent le PaymentSheet côté front. La confirmation effective
 * (PAID + réservation CONFIRMED) est faite par le webhook Stripe.
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
        val intent = paymentGateway.createPaymentIntent(booking.totalPrice, booking.currency, customerId)

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

        return InitiateBookingPaymentResult.Ready(
            clientSecret = intent.clientSecret,
            ephemeralKeySecret = ephemeralKeySecret,
            customerId = customerId,
            publishableKey = paymentGateway.publishableKey(),
            paymentId = payment.id.value,
        )
    }
}

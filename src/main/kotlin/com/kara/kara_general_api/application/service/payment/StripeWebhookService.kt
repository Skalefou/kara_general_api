package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.port.input.payment.HandleStripeWebhookUseCase
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookCommand
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded"

/**
 * Traite les webhooks Stripe : le webhook fait foi. Sur `payment_intent.succeeded`, le paiement passe
 * PAID et la réservation associée passe CONFIRMED. Idempotent : un événement déjà appliqué renvoie
 * simplement Handled.
 */
@Service
class StripeWebhookService(
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
) : HandleStripeWebhookUseCase {

    @Transactional
    override fun handle(command: StripeWebhookCommand): StripeWebhookResult {
        val signature = command.signature ?: return StripeWebhookResult.InvalidSignature
        val event =
            paymentGateway.verifyAndParseWebhook(command.payload, signature)
                ?: return StripeWebhookResult.InvalidSignature

        if (event.type != PAYMENT_INTENT_SUCCEEDED) return StripeWebhookResult.Ignored
        val intentId = event.paymentIntentId ?: return StripeWebhookResult.Ignored

        val payment = paymentRepository.findByStripePaymentIntentId(intentId) ?: return StripeWebhookResult.Ignored
        if (payment.status == PaymentStatus.PAID) return StripeWebhookResult.Handled

        paymentRepository.save(payment.markPaid())
        bookingRepository.updateStatus(payment.bookingId, BookingStatus.CONFIRMED)
        return StripeWebhookResult.Handled
    }
}

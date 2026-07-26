package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.application.service.pool.PoolSettlementService
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
private const val PAYMENT_INTENT_AMOUNT_CAPTURABLE_UPDATED = "payment_intent.amount_capturable_updated"
private const val PAYMENT_INTENT_CANCELED = "payment_intent.canceled"

/**
 * Traite les webhooks Stripe : le webhook fait foi. Deux flux coexistent, distingués par le type d'événement
 * et par la table où l'identifiant de PaymentIntent est retrouvé :
 *
 * - **Payer tout** (`payment_intent.succeeded`, table `payments`) : le paiement passe PAID et la réservation
 *   CONFIRMED.
 * - **Cagnotte** (autorisation à capture manuelle, table `pool_shares`) :
 *   `payment_intent.amount_capturable_updated` → part AUTHORIZED puis, si la cagnotte est complète, capture
 *   globale + réservation CONFIRMED (délégué à [PoolSettlementService]) ; `payment_intent.canceled` → part
 *   CANCELLED.
 *
 * Idempotent : un événement déjà appliqué renvoie simplement Handled. La capture d'une part de cagnotte émet
 * elle-même un `payment_intent.succeeded` : son identifiant est absent de `payments`, l'événement est donc
 * ignoré côté « payer tout » (aucun double traitement).
 */
@Service
class StripeWebhookService(
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
    private val poolSettlementService: PoolSettlementService,
    private val applyBookingExtensionService: ApplyBookingExtensionService,
) : HandleStripeWebhookUseCase {
    @Transactional
    override fun handle(command: StripeWebhookCommand): StripeWebhookResult {
        val signature = command.signature ?: return StripeWebhookResult.InvalidSignature
        val event =
            paymentGateway.verifyAndParseWebhook(command.payload, signature)
                ?: return StripeWebhookResult.InvalidSignature

        val intentId = event.paymentIntentId ?: return StripeWebhookResult.Ignored
        return when (event.type) {
            PAYMENT_INTENT_SUCCEEDED -> handlePayAllSucceeded(intentId)
            PAYMENT_INTENT_AMOUNT_CAPTURABLE_UPDATED -> poolSettlementService.onShareAuthorized(intentId)
            PAYMENT_INTENT_CANCELED -> poolSettlementService.onShareCanceled(intentId)
            else -> StripeWebhookResult.Ignored
        }
    }

    private fun handlePayAllSucceeded(intentId: String): StripeWebhookResult {
        val payment = paymentRepository.findByStripePaymentIntentId(intentId) ?: return StripeWebhookResult.Ignored
        if (payment.status == PaymentStatus.PAID) return StripeWebhookResult.Handled

        paymentRepository.save(payment.markPaid())
        val extensionId = payment.extensionId
        if (extensionId != null) {
            applyBookingExtensionService.apply(extensionId)
        } else {
            bookingRepository.updateStatus(payment.bookingId, BookingStatus.CONFIRMED)
        }
        return StripeWebhookResult.Handled
    }
}

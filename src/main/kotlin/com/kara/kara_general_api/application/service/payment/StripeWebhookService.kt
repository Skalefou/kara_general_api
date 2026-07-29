package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.application.service.pool.PoolSettlementService
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.port.input.payment.HandleStripeWebhookUseCase
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookCommand
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded"
private const val PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed"
private const val PAYMENT_INTENT_AMOUNT_CAPTURABLE_UPDATED = "payment_intent.amount_capturable_updated"
private const val PAYMENT_INTENT_CANCELED = "payment_intent.canceled"

/**
 * Traite les webhooks Stripe. Deux flux coexistent, distingués par le type d'événement et par la table où
 * l'identifiant de PaymentIntent est retrouvé :
 *
 * - **Payer tout** (`payment_intent.succeeded`, table `payments`) : délégué à [ConfirmPayAllPaymentService]
 *   (paiement PAID + réservation CONFIRMED, ou application de l'extension). `payment_intent.payment_failed`
 *   passe le paiement FAILED sans toucher à la réservation, qui reste PENDING et expire normalement.
 * - **Cagnotte** (autorisation à capture manuelle, table `pool_shares`) :
 *   `payment_intent.amount_capturable_updated` → part AUTHORIZED puis, si la cagnotte est complète, capture
 *   globale + réservation CONFIRMED (délégué à [PoolSettlementService]) ; `payment_intent.canceled` → part
 *   CANCELLED.
 *
 * Le webhook n'est plus le **seul** chemin de confirmation : le client peut réconcilier lui-même son
 * paiement (cf. [SyncBookingPaymentService]) si l'événement n'arrive jamais.
 *
 * La capture d'une part de cagnotte émet elle-même un `payment_intent.succeeded` : son identifiant est
 * absent de `payments`, l'événement est donc ignoré côté « payer tout » (aucun double traitement).
 */
@Service
class StripeWebhookService(
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
    private val poolSettlementService: PoolSettlementService,
    private val confirmPayAllPaymentService: ConfirmPayAllPaymentService,
) : HandleStripeWebhookUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun handle(command: StripeWebhookCommand): StripeWebhookResult {
        val signature = command.signature
        if (signature == null) {
            logger.warn("Stripe webhook rejected: the Stripe-Signature header is missing")
            return StripeWebhookResult.InvalidSignature
        }
        val event = paymentGateway.verifyAndParseWebhook(command.payload, signature)
        if (event == null) {
            logger.warn("Stripe webhook rejected: the signature could not be verified")
            return StripeWebhookResult.InvalidSignature
        }

        val intentId = event.paymentIntentId
        if (intentId == null) {
            logger.warn("Stripe webhook ignored: no payment intent id in event type={}", event.type)
            return StripeWebhookResult.Ignored
        }

        return when (event.type) {
            PAYMENT_INTENT_SUCCEEDED -> handlePayAllSucceeded(intentId)
            PAYMENT_INTENT_PAYMENT_FAILED -> handlePayAllFailed(intentId)
            PAYMENT_INTENT_AMOUNT_CAPTURABLE_UPDATED -> poolSettlementService.onShareAuthorized(intentId)
            PAYMENT_INTENT_CANCELED -> poolSettlementService.onShareCanceled(intentId)
            else -> {
                logger.info("Stripe webhook ignored: unhandled event type={}", event.type)
                StripeWebhookResult.Ignored
            }
        }
    }

    private fun handlePayAllSucceeded(intentId: String): StripeWebhookResult {
        val payment = paymentRepository.findByStripePaymentIntentId(intentId)
        if (payment == null) {
            // Cas normal pour la capture d'une part de cagnotte : l'intent vit dans pool_shares, pas payments.
            logger.info("Stripe payment_intent.succeeded ignored: unknown intent in the payments table")
            return StripeWebhookResult.Ignored
        }
        confirmPayAllPaymentService.confirm(payment)
        return StripeWebhookResult.Handled
    }

    private fun handlePayAllFailed(intentId: String): StripeWebhookResult {
        val payment = paymentRepository.findByStripePaymentIntentId(intentId)
        if (payment == null) {
            logger.info("Stripe payment_intent.payment_failed ignored: unknown intent in the payments table")
            return StripeWebhookResult.Ignored
        }
        // La réservation n'est pas touchée : elle reste PENDING et expire par le balayage habituel.
        if (payment.status == PaymentStatus.PENDING) {
            paymentRepository.save(payment.markFailed())
        }
        return StripeWebhookResult.Handled
    }
}

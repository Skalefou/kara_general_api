package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Cœur du règlement de cagnotte piloté par le webhook Stripe (autorisation à capture manuelle).
 *
 * - `amount_capturable_updated` : la part passe AUTHORIZED. Si TOUTES les parts sont autorisées et que leur
 *   somme égale la cible, on **capture** toutes les autorisations, la cagnotte passe SETTLED et la
 *   réservation CONFIRMED, puis le créateur est notifié.
 * - `canceled` : la part passe CANCELLED (autorisation levée, zéro prélèvement).
 *
 * Idempotent : un événement déjà appliqué renvoie Handled sans effet de bord.
 */
@Service
class PoolSettlementService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val paymentGateway: PaymentGateway,
    private val poolNotifier: PoolNotifier,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun onShareAuthorized(intentId: String): StripeWebhookResult {
        val share = poolShareRepository.findByStripePaymentIntentId(intentId) ?: return StripeWebhookResult.Ignored
        if (share.status != PoolShareStatus.PENDING) return StripeWebhookResult.Handled

        poolShareRepository.save(share.markAuthorized())

        val pool = poolRepository.findById(share.poolId) ?: return StripeWebhookResult.Handled
        if (!pool.isOpen()) return StripeWebhookResult.Handled

        val shares = poolShareRepository.findByPoolId(pool.id)
        if (!isComplete(pool, shares)) return StripeWebhookResult.Handled

        settle(pool, shares)
        return StripeWebhookResult.Handled
    }

    /**
     * Annule les autorisations Stripe encore actives d'un lot de parts (AUTHORIZED ou PENDING avec intent)
     * et passe ces parts CANCELLED. Aucun prélèvement. Idempotent et best-effort (une erreur Stripe
     * n'interrompt pas le traitement des autres parts). Exécuté dans la transaction de l'appelant.
     */
    fun cancelShareHolds(shares: List<PoolShare>) {
        shares.forEach { share ->
            val intentId = share.stripePaymentIntentId
            val hasHold = share.status == PoolShareStatus.AUTHORIZED || share.status == PoolShareStatus.PENDING
            if (intentId != null && hasHold) {
                runCatching { paymentGateway.cancelPaymentIntent(intentId) }
                    .onFailure { logger.warn("Failed to cancel Stripe authorization for a pool share") }
                poolShareRepository.save(share.markCancelled())
            }
        }
    }

    /**
     * Rembourse intégralement les parts déjà capturées d'un lot et les passe REFUNDED (annulation d'une
     * réservation confirmée / cagnotte réglée). Best-effort. Exécuté dans la transaction de l'appelant.
     */
    fun refundCapturedShares(shares: List<PoolShare>) {
        shares.forEach { share ->
            val intentId = share.stripePaymentIntentId
            if (share.status == PoolShareStatus.CAPTURED && intentId != null) {
                runCatching { paymentGateway.refundPaymentIntent(intentId) }
                    .onFailure { logger.warn("Failed to refund a captured pool share") }
                poolShareRepository.save(share.markRefunded())
            }
        }
    }

    @Transactional
    fun onShareCanceled(intentId: String): StripeWebhookResult {
        val share = poolShareRepository.findByStripePaymentIntentId(intentId) ?: return StripeWebhookResult.Ignored
        if (share.status == PoolShareStatus.CANCELLED) return StripeWebhookResult.Handled
        if (share.status == PoolShareStatus.CAPTURED) return StripeWebhookResult.Handled
        poolShareRepository.save(share.markCancelled())
        return StripeWebhookResult.Handled
    }

    /** Complète ssi toutes les parts sont autorisées/capturées ET leur somme égale exactement la cible. */
    private fun isComplete(pool: Pool, shares: List<PoolShare>): Boolean {
        if (shares.isEmpty()) return false
        if (!shares.all { it.isSettleable() }) return false
        val sum = shares.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }
        return sum.compareTo(pool.targetAmount) == 0
    }

    private fun settle(pool: Pool, shares: List<PoolShare>) {
        poolRepository.updateStatus(pool.id, PoolStatus.AUTHORIZED_COMPLETE)
        shares
            .filter { it.status == PoolShareStatus.AUTHORIZED }
            .forEach { share ->
                share.stripePaymentIntentId?.let { paymentGateway.capturePaymentIntent(it) }
                poolShareRepository.save(share.markCaptured())
            }
        poolRepository.updateStatus(pool.id, PoolStatus.SETTLED)
        bookingRepository.updateStatus(pool.bookingId, BookingStatus.CONFIRMED)
        bookingRepository.findById(pool.bookingId)?.let { poolNotifier.notifyPoolConfirmed(it) }
    }
}

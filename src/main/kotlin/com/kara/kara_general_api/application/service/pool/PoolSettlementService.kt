package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
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
 * Cœur du règlement de cagnotte. **Unique chemin d'écriture** des transitions de part, appelé par deux
 * entrées : le webhook Stripe ([com.kara.kara_general_api.application.service.payment.StripeWebhookService])
 * et la réconciliation demandée par le front ([SyncPoolShareService]).
 *
 * - autorisation confirmée (`amount_capturable_updated`, ou intent relu en `requires_capture`) : la part
 *   passe AUTHORIZED. Si TOUTES les parts sont autorisées et que leur somme égale la cible, on **capture**
 *   toutes les autorisations, la cagnotte passe SETTLED et la réservation CONFIRMED, puis le créateur est
 *   notifié.
 * - annulation (`canceled`, ou intent relu en `canceled`) : la part passe CANCELLED (autorisation levée,
 *   zéro prélèvement).
 *
 * **Idempotence et concurrence** : chaque transition prend d'abord un verrou pessimiste sur la ligne de la
 * cagnotte ([PoolRepository.findByIdForUpdate]) **puis relit la part**. Deux appels concurrents (webhook et
 * sync qui arrivent en même temps) se sérialisent donc sur ce verrou : le second relit une part déjà
 * AUTHORIZED et ressort sans effet de bord. Aucune double autorisation, aucune double capture, aucune
 * erreur — l'appelant reçoit Handled dans tous les cas.
 */
@Service
class PoolSettlementService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val paymentGateway: PaymentGateway,
    private val poolNotifier: PoolNotifier,
    private val applyBookingExtensionService: ApplyBookingExtensionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun onShareAuthorized(intentId: String): StripeWebhookResult {
        val located = poolShareRepository.findByStripePaymentIntentId(intentId)
        if (located == null) {
            logger.info("Pool share authorization ignored: no share holds this payment intent")
            return StripeWebhookResult.Ignored
        }

        // Verrou pessimiste AVANT toute décision : sérialise le webhook et la réconciliation front.
        val pool = poolRepository.findByIdForUpdate(located.poolId) ?: return StripeWebhookResult.Handled
        // Relecture APRÈS le verrou : une transaction concurrente a pu commiter la transition entre-temps.
        val share = poolShareRepository.findByStripePaymentIntentId(intentId) ?: return StripeWebhookResult.Ignored
        if (share.status != PoolShareStatus.PENDING) {
            logger.info("Pool share authorization already applied (status={}); nothing to do", share.status)
            return StripeWebhookResult.Handled
        }

        poolShareRepository.save(share.markAuthorized())
        logger.info("Pool share authorized for pool {}", pool.id.value)

        if (!pool.isOpen()) return StripeWebhookResult.Handled

        val shares = poolShareRepository.findByPoolId(pool.id)
        if (!isComplete(pool, shares)) return StripeWebhookResult.Handled

        logger.info("Pool {} is fully authorized; capturing {} share(s)", pool.id.value, shares.size)
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
        val located = poolShareRepository.findByStripePaymentIntentId(intentId)
        if (located == null) {
            logger.info("Pool share cancellation ignored: no share holds this payment intent")
            return StripeWebhookResult.Ignored
        }

        // Même verrou que l'autorisation : une annulation ne doit pas croiser un règlement en cours.
        poolRepository.findByIdForUpdate(located.poolId) ?: return StripeWebhookResult.Handled
        val share = poolShareRepository.findByStripePaymentIntentId(intentId) ?: return StripeWebhookResult.Ignored
        if (share.status == PoolShareStatus.CANCELLED) return StripeWebhookResult.Handled
        if (share.status == PoolShareStatus.CAPTURED) return StripeWebhookResult.Handled
        poolShareRepository.save(share.markCancelled())
        logger.info("Pool share cancelled for pool {}", share.poolId.value)
        return StripeWebhookResult.Handled
    }

    /** Complète ssi toutes les parts sont autorisées/capturées ET leur somme égale exactement la cible. */
    private fun isComplete(
        pool: Pool,
        shares: List<PoolShare>,
    ): Boolean {
        if (shares.isEmpty()) return false
        if (!shares.all { it.isSettleable() }) return false
        val sum = shares.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }
        return sum.compareTo(pool.targetAmount) == 0
    }

    private fun settle(
        pool: Pool,
        shares: List<PoolShare>,
    ) {
        poolRepository.updateStatus(pool.id, PoolStatus.AUTHORIZED_COMPLETE)
        shares
            .filter { it.status == PoolShareStatus.AUTHORIZED }
            .forEach { share ->
                // Capture du montant DÛ par la part, jamais du montant autorisé : si l'autorisation porte plus
                // que le dû, le surplus est libéré au lieu d'être prélevé (cf. PaymentGateway).
                share.stripePaymentIntentId?.let { paymentGateway.capturePaymentIntent(it, share.amount) }
                poolShareRepository.save(share.markCaptured())
            }
        poolRepository.updateStatus(pool.id, PoolStatus.SETTLED)

        val extensionId = pool.extensionId
        if (extensionId != null) {
            applyBookingExtensionService.apply(extensionId)
        } else {
            bookingRepository.updateStatus(pool.bookingId, BookingStatus.CONFIRMED)
        }
        bookingRepository.findById(pool.bookingId)?.let { poolNotifier.notifyPoolConfirmed(it) }
    }
}

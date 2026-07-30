package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.SyncPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.SyncPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.SyncPoolShareUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Réconciliation d'une part de cagnotte, pendant de
 * [com.kara.kara_general_api.application.service.payment.SyncBookingPaymentService] pour le mode « payer
 * tout ». On interroge la passerelle pour connaître le statut **réel** du PaymentIntent de la part, puis :
 *
 * - `requires_capture` (fonds bloqués) → même transition que le webhook `amount_capturable_updated`,
 *   **déléguée** à [PoolSettlementService.onShareAuthorized] : part AUTHORIZED puis, si la cagnotte est
 *   complète, capture globale + réservation CONFIRMED ;
 * - `canceled` → même transition que le webhook `payment_intent.canceled`, déléguée à
 *   [PoolSettlementService.onShareCanceled] ;
 * - tout autre statut (paiement encore en attente, échoué, abandonné) → **rien n'est modifié**, exactement
 *   comme le webhook `payment_intent.payment_failed` qui ne touche pas aux parts de cagnotte.
 *
 * Aucune logique de transition ni de règlement n'est dupliquée ici : [PoolSettlementService] reste le seul
 * chemin d'écriture, appelé depuis deux entrées (webhook et sync). L'idempotence et la sécurité vis-à-vis
 * d'un webhook concurrent sont donc celles de ce service (verrou pessimiste sur la cagnotte puis relecture
 * de la part) : rappeler cet endpoint renvoie simplement l'état courant.
 *
 * La vue est **relue après** la transition pour refléter l'état commité dans la même transaction.
 */
@Service
class SyncPoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val paymentGateway: PaymentGateway,
    private val poolSettlementService: PoolSettlementService,
    private val poolRecapAssembler: PoolRecapAssembler,
) : SyncPoolShareUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(command: SyncPoolShareCommand): SyncPoolShareResult {
        val pool = poolRepository.findById(command.poolId) ?: return SyncPoolShareResult.PoolNotFound

        val share = poolShareRepository.findById(command.shareId) ?: return SyncPoolShareResult.ShareNotFound
        if (share.poolId != pool.id) return SyncPoolShareResult.ShareNotFound
        if (!isAllowed(pool, share, command.requesterId)) return SyncPoolShareResult.NotAllowed

        reconcile(share)

        // Relecture : la part et la cagnotte ont pu changer d'état (jusqu'au règlement complet) ci-dessus.
        val refreshedPool = poolRepository.findById(command.poolId) ?: return SyncPoolShareResult.PoolNotFound
        val refreshedShare = poolShareRepository.findById(command.shareId) ?: return SyncPoolShareResult.ShareNotFound
        val view =
            poolRecapAssembler.assemble(refreshedPool, refreshedShare)
                ?: return SyncPoolShareResult.PoolNotFound
        return SyncPoolShareResult.Synced(view)
    }

    /**
     * Accès réservé au payeur de la part et au créateur de la cagnotte (propriétaire de la réservation),
     * cohérent avec les autres endpoints : la lecture complète d'une cagnotte est réservée au créateur
     * (`GetPoolService`), et une part n'appartient qu'à celui qui l'a réglée.
     */
    private fun isAllowed(
        pool: Pool,
        share: PoolShare,
        requesterId: UserId,
    ): Boolean {
        if (share.payerUserId == requesterId) return true
        return bookingRepository.findById(pool.bookingId)?.userId == requesterId
    }

    /** Délègue au chemin d'écriture unique selon le statut réel de l'intent chez la passerelle. */
    private fun reconcile(share: PoolShare) {
        val intentId = share.stripePaymentIntentId
        if (intentId == null) {
            // Part jamais présentée au paiement : rien à réconcilier.
            return
        }
        val snapshot = paymentGateway.retrievePaymentIntent(intentId)
        if (snapshot == null) {
            logger.warn("Pool share reconciliation skipped: the payment intent could not be retrieved")
            return
        }
        logger.info("Pool share reconciliation: gateway reports status={}", snapshot.status)
        when (snapshot.status) {
            PaymentIntentStatus.REQUIRES_CAPTURE -> poolSettlementService.onShareAuthorized(intentId)
            PaymentIntentStatus.CANCELED -> poolSettlementService.onShareCanceled(intentId)
            else -> Unit
        }
    }
}

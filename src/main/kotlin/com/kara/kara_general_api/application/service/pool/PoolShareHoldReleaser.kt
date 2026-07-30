package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Libère l'autorisation Stripe devenue caduque d'une part, et la détache de celle-ci.
 *
 * Deux situations l'exigent :
 *
 * 1. **Le montant dû par la part change.** Une autorisation à capture manuelle est figée sur le montant demandé
 *    à sa création : découpe du reliquat du créateur quand un participant rejoint ([SelfJoinPoolShareService]) ou
 *    est ajouté ([AddPoolShareService]), rééquilibrage par le créateur ([UpdatePoolShareService]). Conserver
 *    l'ancienne autorisation ferait dériver le montant autorisé du montant dû, avec deux conséquences
 *    financières : une capture supérieure au dû si le dû a baissé, et une capture impossible (autorisation trop
 *    faible) si le dû a monté — cagnotte bloquée.
 * 2. **La part est repayée alors qu'elle porte un intent inexploitable** ([AuthorizePoolShareService]) : le
 *    PaymentSheet a échoué et l'intent est resté `requires_action`/`canceled`, ou la passerelle est injoignable.
 *    Le montant ne change pas ici, mais créer un second intent sans libérer le premier l'orphelinerait (l'upsert
 *    d'une part réécrit `stripe_payment_intent_id`) et pourrait laisser deux blocages sur la même carte.
 *
 * On annule donc l'autorisation caduque (blocage libéré, zéro prélèvement) et on la détache de la part, qui
 * redevient une part PENDING sans autorisation : le payeur la règle à nouveau, au bon montant.
 *
 * Best-effort **côté passerelle** (une erreur Stripe ne fait pas échouer l'opération appelante : au pire le
 * blocage expire de lui-même) mais jamais best-effort **en base** : l'identifiant d'intent est détaché dans tous
 * les cas, si bien qu'aucune autorisation obsolète ne peut plus être capturée par [PoolSettlementService].
 *
 * Sans effet sur une part sans autorisation, ni sur une part AUTHORIZED/CAPTURED : celles-là sont protégées en
 * amont par les gardes de statut des services appelants, qui refusent purement et simplement de toucher à une
 * part déjà réglée.
 */
@Component
class PoolShareHoldReleaser(
    private val paymentGateway: PaymentGateway,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Retourne la part débarrassée de son autorisation Stripe caduque (annulée côté passerelle, détachée en base). */
    fun release(share: PoolShare): PoolShare {
        if (share.status != PoolShareStatus.PENDING) return share
        val intentId = share.stripePaymentIntentId ?: return share

        runCatching { paymentGateway.cancelPaymentIntent(intentId) }
            .onFailure { logger.warn("Failed to cancel the stale Stripe authorization of a pool share", it) }
        logger.info("Stale Stripe authorization detached from pool share {}", share.id.value)
        return share.withoutAuthorizationIntent()
    }
}

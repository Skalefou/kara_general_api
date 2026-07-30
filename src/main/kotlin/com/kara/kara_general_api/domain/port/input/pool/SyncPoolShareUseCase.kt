package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId

data class SyncPoolShareCommand(
    val poolId: PoolId,
    val shareId: PoolShareId,
    val requesterId: UserId,
)

sealed interface SyncPoolShareResult {
    /**
     * Réconciliation effectuée : [view] porte l'état **à jour** de la part et de la cagnotte (progression,
     * montant collecté, statut global), de quoi rafraîchir l'écran sans second appel.
     */
    data class Synced(
        val view: PoolRecapView,
    ) : SyncPoolShareResult

    data object PoolNotFound : SyncPoolShareResult

    data object ShareNotFound : SyncPoolShareResult

    /** Le demandeur n'est ni le payeur de la part, ni le créateur de la cagnotte. */
    data object NotAllowed : SyncPoolShareResult
}

/**
 * Réconciliation d'une part de cagnotte : interroge la passerelle pour connaître le statut **réel** du
 * PaymentIntent de la part et applique la même transition que le webhook Stripe si celui-ci n'est jamais
 * arrivé (tunnel absent en développement, livraison retardée ou redéploiement en production).
 *
 * Ne crée jamais de paiement : c'est une lecture chez la passerelle suivie, le cas échéant, de la
 * transition déjà pilotée par le webhook. Sûr à appeler en boucle.
 */
interface SyncPoolShareUseCase {
    fun sync(command: SyncPoolShareCommand): SyncPoolShareResult
}

package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.output.PoolLinkBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/** Vue de gestion d'une cagnotte (« Gérer l'événement ») : cible, montant collecté (autorisé), parts. */
data class PoolView(
    val poolId: UUID,
    val bookingId: UUID,
    val status: PoolStatus,
    val targetAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val currency: Currency,
    val percentage: Int,
    val deadline: Instant,
    val globalLinkToken: String,
    /** Lien de partage global prêt à l'emploi, construit par le serveur : `{base}/join/{globalLinkToken}`. */
    val globalShareUrl: String,
    val shares: List<PoolShareView>,
) {
    companion object {
        fun of(
            pool: Pool,
            shares: List<PoolShare>,
            links: PoolLinkBuilder,
        ): PoolView {
            val collected = collectedAmount(shares)
            return PoolView(
                poolId = pool.id.value,
                bookingId = pool.bookingId.value,
                status = pool.status,
                targetAmount = pool.targetAmount,
                collectedAmount = collected,
                currency = pool.currency,
                percentage = percentage(collected, pool.targetAmount),
                deadline = pool.deadline,
                globalLinkToken = pool.globalLinkToken,
                globalShareUrl = links.globalShareUrl(pool.globalLinkToken),
                shares = shares.map { PoolShareView.of(it, links) },
            )
        }
    }
}

data class PoolShareView(
    val shareId: UUID,
    val participantName: String,
    val email: String?,
    val amount: BigDecimal,
    val status: PoolShareStatus,
    val isCreatorShare: Boolean,
    val uniqueLinkToken: String?,
    /**
     * Lien de partage unique de la part, construit par le serveur : `{base}/p/{uniqueLinkToken}`.
     * `null` quand la part n'a pas de token de lien unique (part reliquat du créateur).
     */
    val shareUrl: String?,
) {
    companion object {
        fun of(
            share: PoolShare,
            links: PoolLinkBuilder,
        ): PoolShareView =
            PoolShareView(
                shareId = share.id.value,
                participantName = share.participantName,
                email = share.email?.value,
                amount = share.amount,
                status = share.status,
                isCreatorShare = share.isCreatorShare,
                uniqueLinkToken = share.uniqueLinkToken,
                shareUrl = share.uniqueLinkToken?.let { links.shareUrl(it) },
            )
    }
}

/**
 * Récapitulatif public d'une cagnotte (lecture sans authentification). Contient le résumé de la
 * réservation et, pour un lien de part, les informations de la part concernée.
 */
data class PoolRecapView(
    val poolId: UUID,
    val status: PoolStatus,
    val roomName: String,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val targetAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val currency: Currency,
    val percentage: Int,
    val deadline: Instant,
    // Renseignés uniquement pour un récapitulatif accédé via un lien de part (share token) :
    val shareId: UUID?,
    val shareParticipantName: String?,
    val shareAmount: BigDecimal?,
    val shareStatus: PoolShareStatus?,
)

/** Montant collecté = somme des parts autorisées ou capturées (fonds effectivement engagés). */
internal fun collectedAmount(shares: List<PoolShare>): BigDecimal =
    shares.filter { it.isSettleable() }.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }

internal fun percentage(
    collected: BigDecimal,
    target: BigDecimal,
): Int =
    if (target <= BigDecimal.ZERO) {
        0
    } else {
        collected.multiply(BigDecimal(100)).divide(target, 0, RoundingMode.DOWN).toInt()
    }

package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.pool.PoolShareView
import com.kara.kara_general_api.domain.port.input.pool.PoolSummaryView
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Vue de gestion d'une cagnotte (« Gérer l'événement »). */
data class PoolResponse(
    val poolId: UUID,
    val bookingId: UUID,
    val status: PoolStatus,
    val targetAmount: BigDecimal,
    @field:Schema(description = "Somme des parts autorisées ou capturées (fonds engagés)")
    val collectedAmount: BigDecimal,
    val currency: Currency,
    @field:Schema(description = "Pourcentage collecté (0-100, arrondi vers le bas)")
    val percentage: Int,
    val deadline: Instant,
    val globalLinkToken: String,
    @field:Schema(
        description = "Lien de partage global prêt à l'emploi (construit par le serveur, à partager tel quel)",
        example = "https://link.karapi.fr/join/8f14e45fceea167a5a36dedd4bea2543",
    )
    val globalShareUrl: String,
    val shares: List<PoolShareResponse>,
) {
    companion object {
        fun from(view: PoolView): PoolResponse =
            PoolResponse(
                poolId = view.poolId,
                bookingId = view.bookingId,
                status = view.status,
                targetAmount = view.targetAmount,
                collectedAmount = view.collectedAmount,
                currency = view.currency,
                percentage = view.percentage,
                deadline = view.deadline,
                globalLinkToken = view.globalLinkToken,
                globalShareUrl = view.globalShareUrl,
                shares = view.shares.map { PoolShareResponse.from(it) },
            )
    }
}

/** Résumé d'une cagnotte pour l'onglet « Mes événements » (liste). */
data class PoolSummaryResponse(
    val poolId: UUID,
    val bookingId: UUID,
    val roomName: String,
    val startAt: Instant,
    val status: PoolStatus,
    val targetAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val currency: Currency,
    val percentage: Int,
    val deadline: Instant,
    @field:Schema(description = "Vrai si l'utilisateur est le créateur (propriétaire de la réservation)")
    val isCreator: Boolean,
) {
    companion object {
        fun from(view: PoolSummaryView): PoolSummaryResponse =
            PoolSummaryResponse(
                poolId = view.poolId,
                bookingId = view.bookingId,
                roomName = view.roomName,
                startAt = view.startAt,
                status = view.status,
                targetAmount = view.targetAmount,
                collectedAmount = view.collectedAmount,
                currency = view.currency,
                percentage = view.percentage,
                deadline = view.deadline,
                isCreator = view.isCreator,
            )
    }
}

data class PoolShareResponse(
    val shareId: UUID,
    val participantName: String,
    val email: String?,
    val amount: BigDecimal,
    val status: PoolShareStatus,
    val isCreatorShare: Boolean,
    @field:Schema(description = "Token de lien unique (présent seulement si un email a été fourni)")
    val uniqueLinkToken: String?,
    @field:Schema(
        description =
            "Lien de partage unique de la part, prêt à l'emploi (construit par le serveur). " +
                "Null quand la part n'a pas de token de lien unique.",
        example = "https://link.karapi.fr/p/c4ca4238a0b923820dcc509a6f75849b",
    )
    val shareUrl: String?,
) {
    companion object {
        fun from(view: PoolShareView): PoolShareResponse =
            PoolShareResponse(
                shareId = view.shareId,
                participantName = view.participantName,
                email = view.email,
                amount = view.amount,
                status = view.status,
                isCreatorShare = view.isCreatorShare,
                uniqueLinkToken = view.uniqueLinkToken,
                shareUrl = view.shareUrl,
            )
    }
}

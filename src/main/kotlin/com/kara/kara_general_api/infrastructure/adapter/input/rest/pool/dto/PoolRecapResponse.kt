package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.pool.PoolRecapView
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

const val POOL_CHARGE_MESSAGE: String =
    "Votre carte n'est débitée que lorsque toutes les parts de la cagnotte ont été payées. " +
        "Sinon, la cagnotte est annulée et personne n'est prélevé."

/** Récapitulatif public d'une cagnotte (accès lecture sans authentification). */
data class PoolRecapResponse(
    val poolId: UUID,
    val status: PoolStatus,
    @field:Schema(description = "Résumé de la réservation")
    val booking: BookingSummary,
    val targetAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val currency: Currency,
    val percentage: Int,
    val deadline: Instant,
    @field:Schema(
        description =
            "Part concernée. Présente dans deux cas seulement : accès par un lien de part, ou accès par le " +
                "lien global avec un JWT valide dont le porteur détient une part de cette cagnotte — sa part, " +
                "jamais celle d'un autre participant. Absente pour un invité, et pour un utilisateur " +
                "authentifié sans part. Son `shareId` permet de reprendre un paiement interrompu via " +
                "POST /api/v1/pools/{poolId}/shares/{shareId}/payment.",
    )
    val share: ShareSummary?,
    @field:Schema(description = "Message expliquant que la carte n'est débitée qu'une fois tout le monde payé")
    val message: String,
) {
    data class BookingSummary(
        val roomName: String,
        val startAt: Instant,
        val endAt: Instant,
        val numberOfPeople: Int,
    )

    data class ShareSummary(
        val shareId: UUID,
        val participantName: String,
        val amount: BigDecimal,
        val status: PoolShareStatus,
    )

    companion object {
        fun from(view: PoolRecapView): PoolRecapResponse =
            PoolRecapResponse(
                poolId = view.poolId,
                status = view.status,
                booking =
                    BookingSummary(
                        roomName = view.roomName,
                        startAt = view.startAt,
                        endAt = view.endAt,
                        numberOfPeople = view.numberOfPeople,
                    ),
                targetAmount = view.targetAmount,
                collectedAmount = view.collectedAmount,
                currency = view.currency,
                percentage = view.percentage,
                deadline = view.deadline,
                share =
                    view.shareId?.let {
                        ShareSummary(
                            shareId = it,
                            participantName = view.shareParticipantName ?: "",
                            amount = view.shareAmount ?: BigDecimal.ZERO,
                            status = view.shareStatus ?: PoolShareStatus.PENDING,
                        )
                    },
                message = POOL_CHARGE_MESSAGE,
            )
    }
}

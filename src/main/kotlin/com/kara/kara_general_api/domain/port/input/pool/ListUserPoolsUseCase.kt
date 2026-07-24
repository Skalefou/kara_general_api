package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Résumé d'une cagnotte pour l'onglet « Mes événements » (liste, sans le détail des parts). */
data class PoolSummaryView(
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
    val isCreator: Boolean,
)

/**
 * Liste les cagnottes auxquelles l'utilisateur authentifié participe : celles qu'il a créées (propriétaire
 * de la réservation) OU dont il détient une part (payeur). Triées par échéance décroissante.
 */
interface ListUserPoolsUseCase {
    fun listForUser(userId: UserId): List<PoolSummaryView>
}

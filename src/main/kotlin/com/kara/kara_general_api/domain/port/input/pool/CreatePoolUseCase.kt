package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal

/** Une part demandée à la création de la cagnotte. Le reliquat du créateur est une part comme les autres. */
data class CreatePoolShareInput(
    val participantName: String,
    val email: String?,
    val amount: BigDecimal,
    val isCreatorShare: Boolean = false,
)

data class CreatePoolCommand(
    val bookingId: BookingId,
    val creatorId: UserId,
    val shares: List<CreatePoolShareInput>,
)

sealed interface CreatePoolResult {
    data class Created(val view: PoolView) : CreatePoolResult

    data object BookingNotFound : CreatePoolResult

    data object NotOwner : CreatePoolResult

    /** La réservation n'est pas en attente de paiement (PENDING). */
    data object BookingNotPending : CreatePoolResult

    /** La réservation n'a pas été créée en mode cagnotte (sharedPot). */
    data object NotSharedPot : CreatePoolResult

    /** Une cagnotte existe déjà pour cette réservation. */
    data object PoolAlreadyExists : CreatePoolResult

    /** La réservation débute trop tôt pour ouvrir une cagnotte (délai calculé déjà échu). */
    data object ReservationTooClose : CreatePoolResult

    /** La somme des parts ne correspond pas au montant cible (= prix total de la réservation). */
    data class SharesMismatch(val expected: BigDecimal, val actual: BigDecimal) : CreatePoolResult

    /** Aucune part fournie, ou montant non strictement positif. */
    data object InvalidShares : CreatePoolResult
}

interface CreatePoolUseCase {
    fun create(command: CreatePoolCommand): CreatePoolResult
}

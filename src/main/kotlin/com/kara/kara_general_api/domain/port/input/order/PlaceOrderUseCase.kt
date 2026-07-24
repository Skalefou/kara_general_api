package com.kara.kara_general_api.domain.port.input.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.user.UserId

data class PlaceOrderCommand(
    val bookingId: BookingId,
    val productId: ProductId,
    val quantity: Int,
    val currentUserId: UserId,
)

sealed interface PlaceOrderResult {
    /** Commande enregistrée et stock décrémenté. */
    data class Success(val order: Order) : PlaceOrderResult

    data object BookingNotFound : PlaceOrderResult

    /** La réservation n'appartient pas au client courant. */
    data object NotOwner : PlaceOrderResult

    /** La réservation n'est pas active (statut non CONFIRMED, ou instant courant hors du créneau). */
    data object BookingNotActive : PlaceOrderResult

    data object ProductNotFound : PlaceOrderResult

    /** Stock de la salle insuffisant pour la quantité demandée. */
    data object InsufficientStock : PlaceOrderResult

    /**
     * Aucun moyen de paiement enregistré : rien n'est persisté ni décrémenté. Le client doit d'abord mettre
     * en place un moyen de paiement (débité automatiquement par la branche paiement).
     */
    data object PaymentMethodRequired : PlaceOrderResult
}

interface PlaceOrderUseCase {
    fun placeOrder(command: PlaceOrderCommand): PlaceOrderResult
}

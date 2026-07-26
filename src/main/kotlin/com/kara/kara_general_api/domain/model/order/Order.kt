package com.kara.kara_general_api.domain.model.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant

/**
 * Commande d'un produit passée par le client pendant une réservation active. Le prix unitaire [unitPrice]
 * est figé au tarif du produit au moment de la commande ; le total [totalPrice] vaut [unitPrice] × [quantity].
 *
 * Le débit du moyen de paiement (capture) et un éventuel remboursement (crédit) ne vivent pas ici : ils sont
 * pilotés par la branche paiement. Cet agrégat ne fait qu'acter la consommation (décrément de stock côté
 * service) et conserver la trace de la commande.
 */
data class Order(
    val id: OrderId,
    val bookingId: BookingId,
    val userId: UserId,
    val productId: ProductId,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val currency: Currency,
    val totalPrice: BigDecimal,
    val status: OrderStatus,
    val createdAt: Instant,
) {
    init {
        require(quantity > 0) { "La quantité commandée doit être strictement positive" }
        require(totalPrice.compareTo(unitPrice.multiply(BigDecimal(quantity))) == 0) {
            "Le prix total doit être égal au prix unitaire multiplié par la quantité"
        }
    }
}

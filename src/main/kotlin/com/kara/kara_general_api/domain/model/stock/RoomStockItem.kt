package com.kara.kara_general_api.domain.model.stock

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId

/**
 * Ligne de stock d'une salle : quantité disponible d'un produit du catalogue générique pour une salle
 * donnée. Une quantité de 0 signifie que le produit est référencé mais épuisé (donc non vendable).
 */
data class RoomStockItem(
    val roomId: RoomId,
    val productId: ProductId,
    val quantity: Int,
) {
    init {
        require(quantity >= 0) { "La quantité en stock doit être positive ou nulle" }
    }
}

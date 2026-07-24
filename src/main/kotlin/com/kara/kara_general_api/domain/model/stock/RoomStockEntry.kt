package com.kara.kara_general_api.domain.model.stock

import com.kara.kara_general_api.domain.model.product.Product

/**
 * Vue enrichie d'une ligne de stock : le produit complet du catalogue générique accompagné de sa
 * quantité disponible dans la salle. Utilisée pour restituer le stock d'une salle.
 */
data class RoomStockEntry(
    val product: Product,
    val quantity: Int,
)

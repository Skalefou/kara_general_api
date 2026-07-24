package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.stock.RoomStockItem

interface RoomStockRepository {
    /** Stock complet d'une salle (produit + quantité), ordonné par nom de produit. */
    fun findByRoomId(roomId: RoomId): List<RoomStockEntry>

    /** Insère ou met à jour la quantité d'un produit pour une salle (upsert sur (room_id, product_id)). */
    fun upsert(item: RoomStockItem)

    /** Retire un produit du stock d'une salle. Retourne true si une ligne a été supprimée. */
    fun deleteByRoomIdAndProductId(roomId: RoomId, productId: ProductId): Boolean
}

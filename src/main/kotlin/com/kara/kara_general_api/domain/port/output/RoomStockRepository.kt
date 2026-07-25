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

    /**
     * Quantité disponible d'un produit dans le stock d'une salle, ou `null` si le produit n'y est pas
     * référencé (jamais mis en stock pour cette salle). Une valeur 0 signifie « référencé mais épuisé ».
     */
    fun findQuantity(roomId: RoomId, productId: ProductId): Int?

    /**
     * Décrémente la quantité d'un produit de façon atomique : la ligne n'est mise à jour que si le
     * stock restant couvre [quantity]. Retourne false si le stock est insuffisant ou le produit absent.
     */
    fun tryDecrement(roomId: RoomId, productId: ProductId, quantity: Int): Boolean

    /** Retire un produit du stock d'une salle. Retourne true si une ligne a été supprimée. */
    fun deleteByRoomIdAndProductId(roomId: RoomId, productId: ProductId): Boolean
}

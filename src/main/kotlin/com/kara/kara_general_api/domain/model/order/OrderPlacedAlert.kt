package com.kara.kara_general_api.domain.model.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import java.math.BigDecimal
import java.time.Instant

/**
 * Alerte temps réel émise vers le serveur lorsqu'un client passe une commande : le serveur doit
 * préparer et apporter le produit. Diffusée par STOMP au serveur de service de la réservation.
 */
data class OrderPlacedAlert(
    val orderId: OrderId,
    val bookingId: BookingId,
    val roomId: RoomId,
    val productName: String,
    val quantity: Int,
    val totalPrice: BigDecimal,
    val currency: Currency,
    val placedAt: Instant,
)

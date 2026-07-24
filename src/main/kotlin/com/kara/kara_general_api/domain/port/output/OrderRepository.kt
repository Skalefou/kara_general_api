package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.order.Order

interface OrderRepository {
    /** Persiste une commande (insert). */
    fun save(order: Order): Order

    /** Commandes rattachées à une réservation, ordonnées par date de création croissante. */
    fun findByBookingId(bookingId: BookingId): List<Order>
}

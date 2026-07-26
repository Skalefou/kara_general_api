package com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto

import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.model.order.OrderStatus
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    @field:Schema(description = "Identifiant de la commande")
    val id: UUID,
    @field:Schema(description = "Identifiant de la réservation")
    val bookingId: UUID,
    @field:Schema(description = "Identifiant du produit commandé")
    val productId: UUID,
    @field:Schema(description = "Quantité commandée", example = "2")
    val quantity: Int,
    @field:Schema(description = "Prix unitaire figé au moment de la commande", example = "2.50")
    val unitPrice: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
    @field:Schema(description = "Prix total (prix unitaire × quantité)", example = "5.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Statut de la commande", example = "PLACED")
    val status: OrderStatus,
    @field:Schema(description = "Date de création (ISO 8601, UTC)")
    val createdAt: Instant,
) {
    companion object {
        fun from(order: Order): OrderResponse =
            OrderResponse(
                id = order.id.value,
                bookingId = order.bookingId.value,
                productId = order.productId.value,
                quantity = order.quantity,
                unitPrice = order.unitPrice,
                currency = order.currency,
                totalPrice = order.totalPrice,
                status = order.status,
                createdAt = order.createdAt,
            )
    }
}

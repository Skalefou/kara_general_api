package com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

data class AvailableProductResponse(
    @field:Schema(description = "Identifiant du produit")
    val productId: UUID,
    @field:Schema(description = "Nom du produit", example = "Coca-Cola 33cl")
    val name: String,
    @field:Schema(description = "Description du produit", example = "Canette 33cl")
    val description: String?,
    @field:Schema(description = "Prix unitaire du produit", example = "2.50")
    val price: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
    @field:Schema(description = "Quantité disponible dans la salle", example = "24")
    val quantity: Int,
) {
    companion object {
        fun from(entry: RoomStockEntry): AvailableProductResponse =
            AvailableProductResponse(
                productId = entry.product.id.value,
                name = entry.product.name,
                description = entry.product.description,
                price = entry.product.price,
                currency = entry.product.currency,
                quantity = entry.quantity,
            )
    }
}

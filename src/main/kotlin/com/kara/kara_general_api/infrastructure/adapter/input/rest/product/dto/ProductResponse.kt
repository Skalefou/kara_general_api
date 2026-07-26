package com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

data class ProductResponse(
    @field:Schema(description = "Identifiant unique du produit")
    val id: UUID,
    @field:Schema(description = "Nom du produit", example = "Coca-Cola 33cl")
    val name: String,
    @field:Schema(description = "Description du produit", example = "Canette 33cl")
    val description: String?,
    @field:Schema(description = "Prix unitaire du produit", example = "2.50")
    val price: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
) {
    companion object {
        fun from(product: Product): ProductResponse =
            ProductResponse(
                id = product.id.value,
                name = product.name,
                description = product.description,
                price = product.price,
                currency = product.currency,
            )
    }
}

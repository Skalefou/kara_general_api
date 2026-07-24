package com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto

import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank
    @field:Schema(description = "Nom du produit", example = "Coca-Cola 33cl")
    val name: String,
    @field:Schema(description = "Description du produit", example = "Canette 33cl")
    val description: String?,
    @field:NotNull
    @field:DecimalMin(value = "0.0", message = "Le prix du produit doit être positif")
    @field:Schema(description = "Prix unitaire du produit", example = "2.50")
    val price: BigDecimal,
    @field:NotNull
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
)

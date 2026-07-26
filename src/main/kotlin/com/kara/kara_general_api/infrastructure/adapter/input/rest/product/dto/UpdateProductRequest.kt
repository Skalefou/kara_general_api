package com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto

import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal

/**
 * Mise à jour partielle d'un produit : tout champ omis (null) laisse la valeur existante inchangée.
 */
data class UpdateProductRequest(
    @field:Schema(description = "Nom du produit", example = "Coca-Cola 33cl")
    val name: String? = null,
    @field:Schema(description = "Description du produit", example = "Canette 33cl")
    val description: String? = null,
    @field:DecimalMin(value = "0.0", message = "Le prix du produit doit être positif")
    @field:Schema(description = "Prix unitaire du produit", example = "2.50")
    val price: BigDecimal? = null,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency? = null,
)

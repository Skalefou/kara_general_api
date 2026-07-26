package com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class PlaceOrderRequest(
    @field:NotNull
    @field:Schema(description = "Identifiant du produit commandé", requiredMode = Schema.RequiredMode.REQUIRED)
    val productId: UUID,
    @field:Min(value = 1, message = "La quantité commandée doit être au minimum de 1")
    @field:Schema(description = "Quantité commandée", example = "2", minimum = "1")
    val quantity: Int,
)

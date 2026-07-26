package com.kara.kara_general_api.infrastructure.adapter.input.rest.stock.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class SetRoomStockRequest(
    @field:NotNull
    @field:Min(value = 0, message = "La quantité en stock doit être positive ou nulle")
    @field:Schema(description = "Quantité disponible du produit dans la salle", example = "24")
    val quantity: Int,
)

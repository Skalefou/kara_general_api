package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomOption
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

data class RoomOptionResponse(
    @field:Schema(description = "Identifiant unique de l'option")
    val id: UUID,
    @field:Schema(description = "Libellé de l'option", example = "Ménage fin de soirée")
    val label: String,
    @field:Schema(description = "Description de l'option", example = "Nettoyage complet après l'événement")
    val description: String?,
    @field:Schema(description = "Prix forfaitaire fixe de l'option", example = "60.00")
    val price: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
) {
    companion object {
        fun from(option: RoomOption): RoomOptionResponse =
            RoomOptionResponse(
                id = option.id.value,
                label = option.label,
                description = option.description,
                price = option.price,
                currency = option.currency,
            )
    }
}

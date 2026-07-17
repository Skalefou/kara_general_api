package com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

data class ServiceResponse(
    @field:Schema(description = "Identifiant unique du service")
    val id: UUID,
    @field:Schema(description = "Libellé du service", example = "Ménage fin de soirée")
    val label: String,
    @field:Schema(description = "Description du service", example = "Nettoyage complet après l'événement")
    val description: String?,
    @field:Schema(description = "Prix forfaitaire fixe du service", example = "60.00")
    val price: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
) {
    companion object {
        fun from(service: Service): ServiceResponse =
            ServiceResponse(
                id = service.id.value,
                label = service.label,
                description = service.description,
                price = service.price,
                currency = service.currency,
            )
    }
}

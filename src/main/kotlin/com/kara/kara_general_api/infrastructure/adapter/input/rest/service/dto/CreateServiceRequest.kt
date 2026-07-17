package com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto

import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateServiceRequest(
    @field:NotBlank
    @field:Schema(description = "Libellé du service", example = "Ménage fin de soirée")
    val label: String,
    @field:Schema(description = "Description du service", example = "Nettoyage complet après l'événement")
    val description: String?,
    @field:NotNull
    @field:DecimalMin(value = "0.0", message = "Le prix du service doit être positif")
    @field:Schema(description = "Prix forfaitaire fixe du service", example = "60.00")
    val price: BigDecimal,
    @field:NotNull
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
)

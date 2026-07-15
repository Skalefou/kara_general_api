package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class CreateRoomRequest(
    @field:NotBlank
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "Adresse de la salle", example = "12 rue de la Paix")
    val street: String,
    @field:NotBlank
    @field:Schema(description = "Ville", example = "Paris")
    val city: String,
    @field:NotBlank
    @field:Schema(description = "Code postal", example = "75002")
    val postalCode: String,
    @field:NotBlank
    @field:Schema(description = "Pays", example = "France")
    val country: String,
    @field:NotNull
    @field:PositiveOrZero
    @field:Schema(description = "Prix par personne et par heure", example = "12.50")
    val pricePerPersonPerHour: BigDecimal,
    @field:NotNull
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
)

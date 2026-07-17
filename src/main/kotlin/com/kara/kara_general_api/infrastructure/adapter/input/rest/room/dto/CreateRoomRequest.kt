package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.util.UUID

data class CreateRoomRequest(
    @field:NotBlank
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "Description de la salle", example = "Grande salle lumineuse avec terrasse")
    val description: String,
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
    @field:NotNull
    @field:Min(2)
    @field:Schema(description = "Capacité maximale (nombre de personnes, minimum 2)", example = "50")
    val maxCapacity: Int,
    @field:NotNull
    @field:Schema(description = "Présence du Wi-Fi", example = "true")
    val thereWifi: Boolean,
    @field:NotNull
    @field:Schema(description = "Présence d'une sono professionnelle", example = "true")
    val thereSonoPro: Boolean,
    @field:NotNull
    @field:Schema(description = "Présence de la climatisation", example = "false")
    val thereAirConditioning: Boolean,
    @field:Schema(
        description = "Identifiants des services du catalogue global à attacher à la salle. " +
            "Optionnel : par défaut aucune liaison n'est créée.",
    )
    val serviceIds: List<UUID> = emptyList(),
)

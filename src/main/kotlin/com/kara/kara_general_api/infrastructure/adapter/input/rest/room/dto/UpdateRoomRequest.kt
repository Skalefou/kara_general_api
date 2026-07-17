package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.util.UUID

data class UpdateRoomRequest(
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String? = null,
    @field:Schema(description = "Description de la salle", example = "Grande salle lumineuse avec terrasse")
    val description: String? = null,
    @field:Schema(description = "Adresse de la salle", example = "12 rue de la Paix")
    val street: String? = null,
    @field:Schema(description = "Ville", example = "Paris")
    val city: String? = null,
    @field:Schema(description = "Code postal", example = "75002")
    val postalCode: String? = null,
    @field:Schema(description = "Pays", example = "France")
    val country: String? = null,
    @field:PositiveOrZero
    @field:Schema(description = "Prix par personne et par heure", example = "12.50")
    val pricePerPersonPerHour: BigDecimal? = null,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency? = null,
    @field:Min(2)
    @field:Schema(description = "Capacité maximale (nombre de personnes, minimum 2)", example = "50")
    val maxCapacity: Int? = null,
    @field:Schema(description = "Présence du Wi-Fi", example = "true")
    val thereWifi: Boolean? = null,
    @field:Schema(description = "Présence d'une sono professionnelle", example = "true")
    val thereSonoPro: Boolean? = null,
    @field:Schema(description = "Présence de la climatisation", example = "false")
    val thereAirConditioning: Boolean? = null,
    @field:Schema(
        description = "Statut de la salle : OPEN (réservable) ou CLOSED (fermée temporairement)",
        example = "CLOSED",
    )
    val status: RoomStatus? = null,
    @field:Schema(
        description = "Identifiants des services du catalogue global attachés à la salle. " +
            "null = liaisons inchangées ; une liste (même vide) remplace l'ensemble des liaisons " +
            "(liste vide = tout détacher).",
    )
    val serviceIds: List<UUID>? = null,
)

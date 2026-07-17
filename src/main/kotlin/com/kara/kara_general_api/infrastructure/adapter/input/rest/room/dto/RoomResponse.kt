package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.model.room.RoomStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class RoomResponse(
    @field:Schema(description = "Identifiant unique de la salle")
    val id: UUID,
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String,
    @field:Schema(description = "Description de la salle", example = "Grande salle lumineuse avec terrasse")
    val description: String,
    @field:Schema(description = "Adresse de la salle", example = "12 rue de la Paix")
    val street: String,
    @field:Schema(description = "Ville", example = "Paris")
    val city: String,
    @field:Schema(description = "Code postal", example = "75002")
    val postalCode: String,
    @field:Schema(description = "Pays", example = "France")
    val country: String,
    @field:Schema(description = "Prix par personne et par heure", example = "12.50")
    val pricePerPersonPerHour: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
    @field:Schema(description = "Capacité maximale (nombre de personnes)", example = "50")
    val maxCapacity: Int,
    @field:Schema(description = "Présence du Wi-Fi", example = "true")
    val thereWifi: Boolean,
    @field:Schema(description = "Présence d'une sono professionnelle", example = "true")
    val thereSonoPro: Boolean,
    @field:Schema(description = "Présence de la climatisation", example = "false")
    val thereAirConditioning: Boolean,
    @field:Schema(description = "Latitude déduite de l'adresse", example = "48.8566")
    val latitude: Double?,
    @field:Schema(description = "Longitude déduite de l'adresse", example = "2.3522")
    val longitude: Double?,
    @field:Schema(description = "Date de création de la salle")
    val createdAt: Instant,
    @field:Schema(description = "Statut de la salle", example = "OPEN")
    val status: RoomStatus,
    @field:Schema(description = "Images publiques de la salle")
    val images: List<RoomImageResponse>,
    @field:Schema(description = "Options tarifées (forfaits fixes) proposées pour la salle")
    val options: List<RoomOptionResponse>,
) {
    companion object {
        fun from(
            room: Room,
            publicUrl: (String) -> String,
            options: List<RoomOption> = emptyList(),
        ): RoomResponse =
            RoomResponse(
                id = room.id.value,
                name = room.name,
                description = room.description,
                street = room.address.street,
                city = room.address.city,
                postalCode = room.address.postalCode,
                country = room.address.country,
                pricePerPersonPerHour = room.pricePerPersonPerHour,
                currency = room.currency,
                maxCapacity = room.maxCapacity,
                thereWifi = room.isThereWifi,
                thereSonoPro = room.isThereSonoPro,
                thereAirConditioning = room.isThereAirConditioning,
                latitude = room.latitude,
                longitude = room.longitude,
                createdAt = room.createdAt,
                status = room.status,
                images = room.images.map { RoomImageResponse.from(it, publicUrl) },
                options = options.map { RoomOptionResponse.from(it) },
            )
    }
}

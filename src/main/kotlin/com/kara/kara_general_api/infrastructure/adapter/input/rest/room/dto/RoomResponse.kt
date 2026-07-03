package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.Room
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class RoomResponse(
    @field:Schema(description = "Identifiant unique de la salle")
    val id: UUID,
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String,
    @field:Schema(description = "Adresse de la salle", example = "12 rue de la Paix")
    val street: String,
    @field:Schema(description = "Ville", example = "Paris")
    val city: String,
    @field:Schema(description = "Code postal", example = "75002")
    val postalCode: String,
    @field:Schema(description = "Pays", example = "France")
    val country: String,
    @field:Schema(description = "Date de création de la salle")
    val createdAt: Instant,
) {
    companion object {
        fun from(room: Room): RoomResponse =
            RoomResponse(
                id = room.id.value,
                name = room.name,
                street = room.address.street,
                city = room.address.city,
                postalCode = room.address.postalCode,
                country = room.address.country,
                createdAt = room.createdAt,
            )
    }
}

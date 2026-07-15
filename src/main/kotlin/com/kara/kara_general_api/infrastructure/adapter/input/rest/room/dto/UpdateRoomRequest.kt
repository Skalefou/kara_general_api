package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.RoomStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class UpdateRoomRequest(
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String? = null,
    @field:Schema(description = "Adresse de la salle", example = "12 rue de la Paix")
    val street: String? = null,
    @field:Schema(description = "Ville", example = "Paris")
    val city: String? = null,
    @field:Schema(description = "Code postal", example = "75002")
    val postalCode: String? = null,
    @field:Schema(description = "Pays", example = "France")
    val country: String? = null,
    @field:PositiveOrZero
    @field:Schema(description = "Prix par personne et par heure (€)", example = "12.50")
    val pricePerPersonPerHour: BigDecimal? = null,
    @field:Schema(
        description = "Statut de la salle : OPEN (réservable) ou CLOSED (fermée temporairement)",
        example = "CLOSED",
    )
    val status: RoomStatus? = null,
)

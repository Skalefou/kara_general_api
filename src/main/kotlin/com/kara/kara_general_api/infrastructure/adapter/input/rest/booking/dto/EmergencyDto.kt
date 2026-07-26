package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import io.swagger.v3.oas.annotations.media.Schema

/** Charge utile d'une alerte d'urgence diffusée aux serveurs via STOMP. */
data class EmergencyDto(
    @field:Schema(description = "Identifiant de la réservation concernée")
    val bookingId: String,
    @field:Schema(description = "Identifiant de la salle")
    val roomId: String,
    @field:Schema(description = "Nom de la salle où se rendre")
    val roomName: String,
    @field:Schema(description = "Message d'alerte")
    val message: String,
    @field:Schema(description = "Horodatage du déclenchement (ISO 8601, UTC)")
    val triggeredAt: String,
)

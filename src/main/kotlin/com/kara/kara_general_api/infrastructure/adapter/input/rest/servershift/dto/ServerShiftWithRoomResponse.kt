package com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto

import com.kara.kara_general_api.domain.model.servershift.ServerShiftWithRoom
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/** Créneau d'agenda enrichi de la salle, destiné à l'agenda personnel du serveur. */
data class ServerShiftWithRoomResponse(
    @field:Schema(description = "Identifiant du créneau")
    val id: UUID,
    @field:Schema(description = "Identifiant de la salle")
    val roomId: UUID,
    @field:Schema(description = "Nom de la salle où se rendre")
    val roomName: String,
    @field:Schema(description = "Ville de la salle")
    val roomCity: String,
    @field:Schema(description = "Début du créneau (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:Schema(description = "Fin du créneau (ISO 8601, UTC)", example = "2026-08-01T23:00:00Z")
    val endAt: Instant,
    @field:Schema(description = "Note optionnelle", nullable = true)
    val note: String?,
) {
    companion object {
        fun from(view: ServerShiftWithRoom): ServerShiftWithRoomResponse =
            ServerShiftWithRoomResponse(
                id = view.shift.id.value,
                roomId = view.shift.roomId.value,
                roomName = view.roomName,
                roomCity = view.roomCity,
                startAt = view.shift.startAt,
                endAt = view.shift.endAt,
                note = view.shift.note,
            )
    }
}

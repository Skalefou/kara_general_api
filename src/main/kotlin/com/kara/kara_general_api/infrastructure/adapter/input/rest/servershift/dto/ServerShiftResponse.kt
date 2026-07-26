package com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto

import com.kara.kara_general_api.domain.model.servershift.ServerShift
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ServerShiftResponse(
    @field:Schema(description = "Identifiant du créneau")
    val id: UUID,
    @field:Schema(description = "Identifiant du serveur affecté")
    val serverId: UUID,
    @field:Schema(description = "Identifiant de la salle")
    val roomId: UUID,
    @field:Schema(description = "Début du créneau (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:Schema(description = "Fin du créneau (ISO 8601, UTC)", example = "2026-08-01T23:00:00Z")
    val endAt: Instant,
    @field:Schema(description = "Note optionnelle", nullable = true)
    val note: String?,
    @field:Schema(description = "Date de création (ISO 8601, UTC)")
    val createdAt: Instant,
) {
    companion object {
        fun from(shift: ServerShift): ServerShiftResponse =
            ServerShiftResponse(
                id = shift.id.value,
                serverId = shift.serverId.value,
                roomId = shift.roomId.value,
                startAt = shift.startAt,
                endAt = shift.endAt,
                note = shift.note,
                createdAt = shift.createdAt,
            )
    }
}

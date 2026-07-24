package com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateServerShiftRequest(
    @field:NotNull
    @field:Schema(description = "Identifiant du serveur affecté")
    val serverId: UUID,
    @field:NotNull
    @field:Schema(description = "Identifiant de la salle où le serveur doit travailler")
    val roomId: UUID,
    @field:NotNull
    @field:Schema(description = "Début du créneau (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:NotNull
    @field:Schema(description = "Fin du créneau (ISO 8601, UTC)", example = "2026-08-01T23:00:00Z")
    val endAt: Instant,
    @field:Size(max = 500)
    @field:Schema(description = "Note optionnelle (consigne, poste…)", example = "Accueil + bar", nullable = true)
    val note: String? = null,
)

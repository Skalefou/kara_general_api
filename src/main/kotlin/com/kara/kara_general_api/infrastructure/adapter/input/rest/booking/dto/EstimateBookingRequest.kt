package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class EstimateBookingRequest(
    @field:NotNull
    @field:Schema(description = "Identifiant de la salle à estimer")
    val roomId: UUID,
    @field:NotNull
    @field:Schema(description = "Début du créneau (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:NotNull
    @field:Schema(description = "Fin du créneau (ISO 8601, UTC)", example = "2026-08-01T21:30:00Z")
    val endAt: Instant,
    @field:NotNull
    @field:Min(2)
    @field:Schema(description = "Nombre de personnes (minimum 2)", example = "8")
    val numberOfPeople: Int,
    @field:Schema(description = "Identifiants des options tarifées retenues", example = "[]")
    val optionIds: List<UUID> = emptyList(),
)

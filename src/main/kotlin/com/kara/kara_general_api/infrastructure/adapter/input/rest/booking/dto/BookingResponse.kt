package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class BookingResponse(
    @field:Schema(description = "Identifiant de la réservation")
    val id: UUID,
    @field:Schema(description = "Identifiant de la salle réservée")
    val roomId: UUID,
    @field:Schema(description = "Identifiant du client ayant créé la réservation")
    val userId: UUID,
    @field:Schema(description = "Début du créneau (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:Schema(description = "Fin du créneau (ISO 8601, UTC)", example = "2026-08-01T21:30:00Z")
    val endAt: Instant,
    @field:Schema(description = "Nombre de personnes", example = "8")
    val numberOfPeople: Int,
    @field:Schema(description = "Identifiants des options tarifées retenues")
    val selectedOptionIds: List<UUID>,
    @field:Schema(description = "Prix total figé à la création (base + options)", example = "385.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
    @field:Schema(description = "Statut de la réservation", example = "PENDING")
    val status: BookingStatus,
    @field:Schema(description = "Date de création (ISO 8601, UTC)")
    val createdAt: Instant,
) {
    companion object {
        fun from(booking: Booking): BookingResponse =
            BookingResponse(
                id = booking.id.value,
                roomId = booking.roomId.value,
                userId = booking.userId.value,
                startAt = booking.startAt,
                endAt = booking.endAt,
                numberOfPeople = booking.numberOfPeople,
                selectedOptionIds = booking.selectedOptionIds.map { it.value },
                totalPrice = booking.totalPrice,
                currency = booking.currency,
                status = booking.status,
                createdAt = booking.createdAt,
            )
    }
}

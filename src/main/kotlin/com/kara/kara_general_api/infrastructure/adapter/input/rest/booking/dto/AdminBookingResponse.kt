package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.AdminBooking
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Récap d'une réservation pour la supervision admin (avec identité du client). */
data class AdminBookingResponse(
    @field:Schema(description = "Identifiant de la réservation")
    val id: UUID,
    @field:Schema(description = "Identifiant de la salle")
    val roomId: UUID,
    @field:Schema(description = "Nom de la salle")
    val roomName: String,
    @field:Schema(description = "Nom du client ayant réservé")
    val clientName: String,
    @field:Schema(description = "Début (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:Schema(description = "Fin (ISO 8601, UTC)", example = "2026-08-01T21:30:00Z")
    val endAt: Instant,
    @field:Schema(description = "Nombre de personnes", example = "8")
    val numberOfPeople: Int,
    @field:Schema(description = "Statut", example = "CONFIRMED")
    val status: BookingStatus,
    @field:Schema(description = "Prix total", example = "385.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Devise (ISO 4217)", example = "EUR")
    val currency: Currency,
) {
    companion object {
        fun from(view: AdminBooking): AdminBookingResponse =
            AdminBookingResponse(
                id = view.booking.id.value,
                roomId = view.booking.roomId.value,
                roomName = view.roomName,
                clientName = view.clientName,
                startAt = view.booking.startAt,
                endAt = view.booking.endAt,
                numberOfPeople = view.booking.numberOfPeople,
                status = view.booking.status,
                totalPrice = view.booking.totalPrice,
                currency = view.booking.currency,
            )
    }
}

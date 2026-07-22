package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.ServerBooking
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Récap d'une réservation pour le serveur rattaché. Aucune donnée personnelle du client. */
data class ServerBookingResponse(
    @field:Schema(description = "Identifiant de la réservation")
    val id: UUID,
    @field:Schema(description = "Identifiant de la salle")
    val roomId: UUID,
    @field:Schema(description = "Nom de la salle")
    val roomName: String,
    @field:Schema(description = "Début (ISO 8601, UTC)", example = "2026-08-01T18:00:00Z")
    val startAt: Instant,
    @field:Schema(description = "Fin (ISO 8601, UTC)", example = "2026-08-01T21:30:00Z")
    val endAt: Instant,
    @field:Schema(description = "Nombre de personnes", example = "8")
    val numberOfPeople: Int,
    @field:Schema(description = "Statut de la réservation", example = "CONFIRMED")
    val status: BookingStatus,
    @field:Schema(description = "Prix total", example = "385.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Devise (ISO 4217)", example = "EUR")
    val currency: Currency,
) {
    companion object {
        fun from(view: ServerBooking): ServerBookingResponse =
            ServerBookingResponse(
                id = view.booking.id.value,
                roomId = view.booking.roomId.value,
                roomName = view.roomName,
                startAt = view.booking.startAt,
                endAt = view.booking.endAt,
                numberOfPeople = view.booking.numberOfPeople,
                status = view.booking.status,
                totalPrice = view.booking.totalPrice,
                currency = view.booking.currency,
            )
    }
}

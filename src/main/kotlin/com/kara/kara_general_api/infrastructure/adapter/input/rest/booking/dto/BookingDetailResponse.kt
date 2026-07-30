package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.booking.BookingDetailView
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Détail d'une réservation + billet, pour son organisateur ou pour un participant détenant une part de sa
 * cagnotte (`isCreator`). `ticketCode` est non-null uniquement lorsque le statut est CONFIRMED.
 */
data class BookingDetailResponse(
    val bookingId: UUID,
    val roomName: String,
    @field:Schema(description = "Adresse formatée de la salle (nulle si la salle est introuvable)")
    val roomAddress: String?,
    @field:Schema(
        description = "Latitude de la salle (nulle si la salle est introuvable ou non géolocalisée)",
        example = "48.8566",
    )
    val roomLatitude: Double?,
    @field:Schema(
        description = "Longitude de la salle (nulle si la salle est introuvable ou non géolocalisée)",
        example = "2.3522",
    )
    val roomLongitude: Double?,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val totalPrice: BigDecimal,
    val currency: Currency,
    val status: BookingStatus,
    val paymentMode: PaymentMode,
    @field:Schema(
        description = "Code de billet lisible (le front en génère le QR). Présent seulement si CONFIRMED, sinon null.",
        example = "KARA-TKT-3F7Q2K9A",
    )
    val ticketCode: String?,
    @field:Schema(
        description =
            "Vrai si l'utilisateur est l'organisateur (propriétaire de la réservation). Faux s'il n'y " +
                "participe qu'en détenant une part de la cagnotte : il est alors en lecture seule sur la " +
                "réservation (aucune annulation, extension, commande ni gestion de cagnotte).",
    )
    val isCreator: Boolean,
) {
    companion object {
        fun from(view: BookingDetailView): BookingDetailResponse =
            BookingDetailResponse(
                bookingId = view.bookingId,
                roomName = view.roomName,
                roomAddress = view.roomAddress,
                roomLatitude = view.roomLatitude,
                roomLongitude = view.roomLongitude,
                startAt = view.startAt,
                endAt = view.endAt,
                numberOfPeople = view.numberOfPeople,
                totalPrice = view.totalPrice,
                currency = view.currency,
                status = view.status,
                paymentMode = view.paymentMode,
                ticketCode = view.ticketCode,
                isCreator = view.isCreator,
            )
    }
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/** Conversation rattachée à une réservation, avec sa date de fermeture (fin + 30 min). */
data class BookingConversationResponse(
    @field:Schema(description = "Identifiant de la conversation à utiliser avec /api/v1/chat/conversations/{id}")
    val conversationId: UUID,
    @field:Schema(description = "Identifiant de la réservation")
    val bookingId: UUID,
    @field:Schema(description = "Fermeture du chat (fin de la réservation + 30 min, ISO 8601, UTC)")
    val closesAt: Instant,
    @field:Schema(description = "Vrai si le chat est déjà fermé (envoi refusé, lecture seule)")
    val closed: Boolean,
) {
    companion object {
        fun from(result: OpenBookingConversationResult.Success): BookingConversationResponse =
            BookingConversationResponse(
                conversationId = result.conversationId.value,
                bookingId = result.bookingId.value,
                closesAt = result.closesAt,
                closed = result.closed,
            )
    }
}

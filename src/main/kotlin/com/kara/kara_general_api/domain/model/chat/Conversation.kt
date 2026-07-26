package com.kara.kara_general_api.domain.model.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
import java.time.Instant

data class Conversation(
    val id: ConversationId,
    val createdAt: Instant,
    val bookingId: BookingId? = null,
) {
    companion object {
        fun create(): Conversation = Conversation(id = ConversationId.generate(), createdAt = Instant.now())

        /** Conversation rattachée à une réservation : elle se ferme 30 min après la fin du créneau. */
        fun createForBooking(bookingId: BookingId): Conversation =
            Conversation(id = ConversationId.generate(), createdAt = Instant.now(), bookingId = bookingId)
    }
}

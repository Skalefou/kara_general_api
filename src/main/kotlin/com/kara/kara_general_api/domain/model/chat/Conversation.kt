package com.kara.kara_general_api.domain.model.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
import java.time.Duration
import java.time.Instant

data class Conversation(
    val id: ConversationId,
    val createdAt: Instant,
    val bookingId: BookingId? = null,
    /** Titre choisi par un participant. Null : le titre est déduit de la réservation ou des participants. */
    val title: String? = null,
) {
    companion object {
        /**
         * Délai après la fin du créneau au-delà duquel la conversation d'une réservation est close :
         * les messages restent lisibles et chacun peut encore retirer les siens, mais plus personne
         * n'écrit ni ne réagit. Une conversation s'ouvre dès la création de la réservation, sans
         * attendre le paiement, pour que le groupe puisse s'organiser.
         */
        val BOOKING_CHAT_WINDOW_AFTER_END: Duration = Duration.ofDays(1)

        fun create(): Conversation = Conversation(id = ConversationId.generate(), createdAt = Instant.now())

        fun createForBooking(bookingId: BookingId): Conversation =
            Conversation(id = ConversationId.generate(), createdAt = Instant.now(), bookingId = bookingId)
    }
}

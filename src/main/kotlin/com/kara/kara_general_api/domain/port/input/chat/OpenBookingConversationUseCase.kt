package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

data class OpenBookingConversationCommand(
    val bookingId: BookingId,
    val currentUserId: UserId,
    val isAdmin: Boolean,
)

sealed interface OpenBookingConversationResult {
    /**
     * Conversation de la réservation prête. [closesAt] = fin de la réservation + 24 h ; passé ce délai,
     * [closed] vaut vrai et l'envoi de messages est refusé (lecture seule).
     */
    data class Success(
        val conversationId: ConversationId,
        val bookingId: BookingId,
        val closesAt: Instant,
        val closed: Boolean,
    ) : OpenBookingConversationResult

    data object BookingNotFound : OpenBookingConversationResult

    /** L'appelant n'est ni le client de la réservation, ni un serveur qui y est rattaché, ni un admin. */
    data object NotAuthorized : OpenBookingConversationResult
}

interface OpenBookingConversationUseCase {
    fun openBookingConversation(command: OpenBookingConversationCommand): OpenBookingConversationResult
}

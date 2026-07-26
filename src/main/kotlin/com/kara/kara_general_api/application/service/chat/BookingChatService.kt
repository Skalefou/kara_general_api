package com.kara.kara_general_api.application.service.chat

import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationResult
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.ChatRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private val BOOKING_CHAT_WINDOW_AFTER_END: Duration = Duration.ofMinutes(30)

/**
 * Ouvre (ou crée) la conversation rattachée à une réservation. Autorisé au client de la réservation, aux
 * serveurs qui y sont rattachés (via leur agenda) et aux administrateurs. À la création, les participants
 * sont le client et les serveurs rattachés ; un serveur rattaché tardivement est ajouté à l'ouverture.
 */
@Service
class BookingChatService(
    private val bookingRepository: BookingRepository,
    private val serverShiftRepository: ServerShiftRepository,
    private val chatRepository: ChatRepository,
) : OpenBookingConversationUseCase {
    @Transactional
    override fun openBookingConversation(command: OpenBookingConversationCommand): OpenBookingConversationResult {
        val booking =
            bookingRepository.findById(command.bookingId) ?: return OpenBookingConversationResult.BookingNotFound

        val assignedServerIds =
            serverShiftRepository.findServerIdsAssignedTo(booking.roomId, booking.startAt, booking.endAt)

        val authorized =
            command.isAdmin ||
                command.currentUserId == booking.userId ||
                command.currentUserId in assignedServerIds
        if (!authorized) return OpenBookingConversationResult.NotAuthorized

        val existing = chatRepository.findConversationByBookingId(command.bookingId)
        val conversationId =
            if (existing != null) {
                if (!chatRepository.isParticipant(existing.id, command.currentUserId)) {
                    chatRepository.addParticipants(existing.id, setOf(command.currentUserId))
                }
                existing.id
            } else {
                val conversation = Conversation.createForBooking(command.bookingId)
                val participants = assignedServerIds + booking.userId + command.currentUserId
                chatRepository.createConversation(conversation, participants)
                conversation.id
            }

        val closesAt = booking.endAt.plus(BOOKING_CHAT_WINDOW_AFTER_END)
        return OpenBookingConversationResult.Success(
            conversationId = conversationId,
            bookingId = command.bookingId,
            closesAt = closesAt,
            closed = Instant.now().isAfter(closesAt),
        )
    }
}

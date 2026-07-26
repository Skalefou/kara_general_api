package com.kara.kara_general_api.application.service.chat

import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.ConversationView
import com.kara.kara_general_api.domain.model.chat.MESSAGE_STATUS_READ
import com.kara.kara_general_api.domain.model.chat.MESSAGE_STATUS_SENT
import com.kara.kara_general_api.domain.model.chat.MESSAGE_TYPE_TEXT
import com.kara.kara_general_api.domain.model.chat.Message
import com.kara.kara_general_api.domain.model.chat.MessageReaction
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.chat.ReactionView
import com.kara.kara_general_api.domain.model.chat.ReplyPreview
import com.kara.kara_general_api.domain.model.chat.TypingEvent
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.displayName
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationResult
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageResult
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesQuery
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesResult
import com.kara.kara_general_api.domain.port.input.chat.GetMessagesUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListAllConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.ListConversationsUseCase
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadCommand
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadResult
import com.kara.kara_general_api.domain.port.input.chat.MarkMessageReadUseCase
import com.kara.kara_general_api.domain.port.input.chat.NotifyTypingCommand
import com.kara.kara_general_api.domain.port.input.chat.NotifyTypingResult
import com.kara.kara_general_api.domain.port.input.chat.NotifyTypingUseCase
import com.kara.kara_general_api.domain.port.input.chat.SendMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionCommand
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.ChatEventPublisher
import com.kara.kara_general_api.domain.port.output.ChatRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private const val PREVIEW_MAX_LENGTH = 80

private val STAFF_ROLES = setOf(UserRole.SERVER, UserRole.ADMIN)

/** Une conversation de réservation se ferme (envoi interdit) 30 min après la fin du créneau. */
private val BOOKING_CHAT_WINDOW_AFTER_END: Duration = Duration.ofMinutes(30)

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val eventPublisher: ChatEventPublisher,
) : CreateConversationUseCase,
    ListConversationsUseCase,
    ListAllConversationsUseCase,
    GetMessagesUseCase,
    SendMessageUseCase,
    ToggleReactionUseCase,
    MarkMessageReadUseCase,
    DeleteMessageUseCase,
    NotifyTypingUseCase {

    @Transactional
    override fun createConversation(command: CreateConversationCommand): CreateConversationResult {
        val participants = command.participantIds + command.currentUserId
        val existing = chatRepository.findConversationByExactParticipants(participants)
        val conversationId =
            if (existing != null) {
                existing.id
            } else {
                val conversation = Conversation.create()
                chatRepository.createConversation(conversation, participants)
                conversation.id
            }
        val users = resolver()
        return CreateConversationResult.Success(
            buildConversationView(conversationId, participants, command.currentUserId, users),
        )
    }

    @Transactional(readOnly = true)
    override fun listConversations(currentUserId: UserId): List<ConversationView> {
        val users = resolver()
        return chatRepository.findConversationsForUser(currentUserId).map { conversation ->
            val participants = chatRepository.findParticipantIds(conversation.id)
            buildConversationView(conversation.id, participants, currentUserId, users)
        }
    }

    @Transactional(readOnly = true)
    override fun listAllConversations(viewerId: UserId): List<ConversationView> {
        val users = resolver()
        return chatRepository.findAllConversations().map { conversation ->
            val participants = chatRepository.findParticipantIds(conversation.id)
            buildConversationView(conversation.id, participants, viewerId, users)
        }
    }

    @Transactional(readOnly = true)
    override fun getMessages(query: GetMessagesQuery): GetMessagesResult {
        chatRepository.findConversationById(query.conversationId) ?: return GetMessagesResult.ConversationNotFound
        if (!query.isAdmin && !chatRepository.isParticipant(query.conversationId, query.currentUserId)) {
            return GetMessagesResult.NotParticipant
        }
        val participants = chatRepository.findParticipantIds(query.conversationId)
        val lastReadAt = chatRepository.findLastReadAtByParticipant(query.conversationId)
        val users = resolver()
        val messages = chatRepository.findMessages(query.conversationId, query.limit, query.before)
        return GetMessagesResult.Success(messages.map { toMessageView(it, participants, lastReadAt, users) })
    }

    @Transactional
    override fun sendMessage(command: SendMessageCommand): SendMessageResult {
        if (command.text.isBlank()) return SendMessageResult.EmptyText
        val conversation =
            chatRepository.findConversationById(command.conversationId)
                ?: return SendMessageResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return SendMessageResult.NotParticipant
        }
        if (isBookingChatClosed(conversation)) return SendMessageResult.ChatClosed
        val message =
            chatRepository.saveMessage(
                Message.create(
                    conversationId = command.conversationId,
                    senderId = command.currentUserId,
                    text = command.text,
                    replyToId = command.replyToId,
                    isForwarded = command.isForwarded,
                ),
            )
        val view = viewOf(message)
        eventPublisher.publishMessage(command.conversationId, view)
        return SendMessageResult.Success(view)
    }

    @Transactional
    override fun toggleReaction(command: ToggleReactionCommand): ToggleReactionResult {
        chatRepository.findConversationById(command.conversationId) ?: return ToggleReactionResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return ToggleReactionResult.NotParticipant
        }
        val message = chatRepository.findMessageById(command.messageId)
        if (message == null || message.conversationId != command.conversationId) {
            return ToggleReactionResult.MessageNotFound
        }
        if (chatRepository.reactionExists(command.messageId, command.currentUserId, command.emoji)) {
            chatRepository.removeReaction(command.messageId, command.currentUserId, command.emoji)
        } else {
            chatRepository.addReaction(MessageReaction(command.messageId, command.currentUserId, command.emoji))
        }
        val view = viewOf(message)
        eventPublisher.publishMessage(command.conversationId, view)
        return ToggleReactionResult.Success(view)
    }

    @Transactional
    override fun markRead(command: MarkMessageReadCommand): MarkMessageReadResult {
        chatRepository.findConversationById(command.conversationId) ?: return MarkMessageReadResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return MarkMessageReadResult.NotParticipant
        }
        val message = chatRepository.findMessageById(command.messageId)
        if (message == null || message.conversationId != command.conversationId) {
            return MarkMessageReadResult.MessageNotFound
        }
        chatRepository.markReadUpTo(command.conversationId, command.currentUserId, message.sentAt)
        return MarkMessageReadResult.Success
    }

    @Transactional
    override fun deleteMessage(command: DeleteMessageCommand): DeleteMessageResult {
        chatRepository.findConversationById(command.conversationId) ?: return DeleteMessageResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return DeleteMessageResult.NotParticipant
        }
        val message = chatRepository.findMessageById(command.messageId)
        if (message == null || message.conversationId != command.conversationId) {
            return DeleteMessageResult.MessageNotFound
        }
        if (message.senderId != command.currentUserId && !command.isAdmin) {
            return DeleteMessageResult.NotAuthor
        }
        chatRepository.deleteMessage(command.messageId)
        return DeleteMessageResult.Success
    }

    override fun notifyTyping(command: NotifyTypingCommand): NotifyTypingResult {
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return NotifyTypingResult.NotParticipant
        }
        val user = userRepository.findById(command.currentUserId)
        eventPublisher.publishTyping(
            TypingEvent(
                conversationId = command.conversationId,
                userId = command.currentUserId,
                userName = user.displayName(),
                isTyping = command.isTyping,
            ),
        )
        return NotifyTypingResult.Success
    }

    private fun buildConversationView(
        conversationId: ConversationId,
        participants: Set<UserId>,
        currentUserId: UserId,
        users: (UserId) -> User?,
    ): ConversationView {
        val conversation = chatRepository.findConversationById(conversationId)
        val counterpartId = participants.firstOrNull { it != currentUserId } ?: currentUserId
        val counterpart = users(counterpartId)
        val lastMessage = chatRepository.findLastMessage(conversationId)
        val lastReadAt = chatRepository.findLastReadAtByParticipant(conversationId)[currentUserId]
        return ConversationView(
            id = conversationId,
            bookingId = conversation?.bookingId,
            counterpartName = counterpart.displayName(),
            counterpartPhotoKey = counterpart?.photoKey,
            lastMessagePreview = lastMessage?.text?.take(PREVIEW_MAX_LENGTH) ?: "",
            lastMessageAt = lastMessage?.sentAt ?: conversation?.createdAt ?: Instant.now(),
            isLastFromMe = lastMessage?.senderId == currentUserId,
            unreadCount = chatRepository.countUnread(conversationId, currentUserId, lastReadAt),
        )
    }

    /** Vue d'un message avec l'état de lecture recalculé depuis la conversation. */
    private fun viewOf(message: Message): MessageView {
        val participants = chatRepository.findParticipantIds(message.conversationId)
        val lastReadAt = chatRepository.findLastReadAtByParticipant(message.conversationId)
        return toMessageView(message, participants, lastReadAt, resolver())
    }

    private fun toMessageView(
        message: Message,
        participants: Set<UserId>,
        lastReadAt: Map<UserId, Instant?>,
        users: (UserId) -> User?,
    ): MessageView {
        val sender = users(message.senderId)
        val reactions =
            chatRepository.findReactions(message.id).map { reaction ->
                ReactionView(
                    emoji = reaction.emoji,
                    userId = reaction.userId,
                    userName = users(reaction.userId).displayName(),
                )
            }
        val replyTo =
            message.replyToId?.let { chatRepository.findMessageById(it) }?.let { replied ->
                ReplyPreview(
                    messageId = replied.id,
                    senderName = users(replied.senderId).displayName(),
                    type = MESSAGE_TYPE_TEXT,
                    preview = replied.text?.take(PREVIEW_MAX_LENGTH) ?: "",
                )
            }
        return MessageView(
            message = message,
            senderName = sender.displayName(),
            senderPhotoKey = sender?.photoKey,
            isStaff = sender != null && sender.role in STAFF_ROLES,
            status = readStatus(message, participants, lastReadAt),
            replyTo = replyTo,
            reactions = reactions,
        )
    }

    /** « read » si tous les autres participants ont lu au moins jusqu'à la date d'envoi, sinon « sent ». */
    private fun readStatus(
        message: Message,
        participants: Set<UserId>,
        lastReadAt: Map<UserId, Instant?>,
    ): String {
        val others = participants - message.senderId
        val readByAll =
            others.isNotEmpty() &&
                others.all { participant ->
                    val readAt = lastReadAt[participant]
                    readAt != null && !readAt.isBefore(message.sentAt)
                }
        return if (readByAll) MESSAGE_STATUS_READ else MESSAGE_STATUS_SENT
    }

    /** Résolveur mémoïsé d'utilisateurs : évite les allers-retours répétés vers le store utilisateur. */
    private fun resolver(): (UserId) -> User? {
        val cache = HashMap<UserId, User?>()
        return { id -> cache.getOrPut(id) { userRepository.findById(id) } }
    }

    /** Vrai si la conversation est rattachée à une réservation dont la fenêtre de chat (fin + 30 min) est échue. */
    private fun isBookingChatClosed(conversation: Conversation): Boolean {
        val bookingId = conversation.bookingId ?: return false
        val booking = bookingRepository.findById(bookingId) ?: return false
        return Instant.now().isAfter(booking.endAt.plus(BOOKING_CHAT_WINDOW_AFTER_END))
    }
}

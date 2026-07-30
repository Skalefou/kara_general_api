package com.kara.kara_general_api.application.service.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
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
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.displayName
import com.kara.kara_general_api.domain.port.input.chat.ConversationDetailView
import com.kara.kara_general_api.domain.port.input.chat.ConversationMemberView
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationResult
import com.kara.kara_general_api.domain.port.input.chat.CreateConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageResult
import com.kara.kara_general_api.domain.port.input.chat.DeleteMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailQuery
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailResult
import com.kara.kara_general_api.domain.port.input.chat.GetConversationDetailUseCase
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
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationResult
import com.kara.kara_general_api.domain.port.input.chat.RenameConversationUseCase
import com.kara.kara_general_api.domain.port.input.chat.SendMessageCommand
import com.kara.kara_general_api.domain.port.input.chat.SendMessageResult
import com.kara.kara_general_api.domain.port.input.chat.SendMessageUseCase
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminCommand
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminResult
import com.kara.kara_general_api.domain.port.input.chat.SetConversationAdminUseCase
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionCommand
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionResult
import com.kara.kara_general_api.domain.port.input.chat.ToggleReactionUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.ChatEventPublisher
import com.kara.kara_general_api.domain.port.output.ChatRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val PREVIEW_MAX_LENGTH = 80

private const val TITLE_MAX_LENGTH = 255

private val TITLE_DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

private val TITLE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

private val STAFF_ROLES = setOf(UserRole.SERVER, UserRole.ADMIN)

/** Une conversation de réservation se ferme (envoi et réaction interdits) 24 h après la fin du créneau. */
@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val eventPublisher: ChatEventPublisher,
) : CreateConversationUseCase,
    RenameConversationUseCase,
    GetConversationDetailUseCase,
    SetConversationAdminUseCase,
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
                if (participants.size > 2) {
                    chatRepository.setParticipantAdmin(conversation.id, command.currentUserId, true)
                }
                conversation.id
            }
        val users = resolver()
        return CreateConversationResult.Success(
            buildConversationView(conversationId, participants, command.currentUserId, users),
        )
    }

    @Transactional
    override fun renameConversation(command: RenameConversationCommand): RenameConversationResult {
        val conversation =
            chatRepository.findConversationById(command.conversationId)
                ?: return RenameConversationResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return RenameConversationResult.NotParticipant
        }
        val participants = chatRepository.findParticipantIds(command.conversationId)
        if (!canRename(conversation, participants, command.currentUserId)) {
            return RenameConversationResult.NotRenamable
        }
        val title = command.title?.trim()?.takeIf { it.isNotEmpty() }
        if (title != null && title.length > TITLE_MAX_LENGTH) {
            return RenameConversationResult.TitleTooLong
        }

        chatRepository.updateConversationTitle(command.conversationId, title)

        return RenameConversationResult.Success(
            buildConversationView(command.conversationId, participants, command.currentUserId, resolver()),
        )
    }

    @Transactional(readOnly = true)
    override fun getConversationDetail(query: GetConversationDetailQuery): GetConversationDetailResult {
        val conversation =
            chatRepository.findConversationById(query.conversationId)
                ?: return GetConversationDetailResult.ConversationNotFound
        if (!query.isAdminRole && !chatRepository.isParticipant(query.conversationId, query.currentUserId)) {
            return GetConversationDetailResult.NotParticipant
        }
        return GetConversationDetailResult.Success(
            buildConversationDetail(conversation, query.currentUserId),
        )
    }

    @Transactional
    override fun setConversationAdmin(command: SetConversationAdminCommand): SetConversationAdminResult {
        val conversation =
            chatRepository.findConversationById(command.conversationId)
                ?: return SetConversationAdminResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return SetConversationAdminResult.NotParticipant
        }
        val participants = chatRepository.findParticipantIds(command.conversationId)
        if (!isGroupAdmin(conversation, participants, command.currentUserId)) {
            return SetConversationAdminResult.NotAdmin
        }
        if (command.memberId !in participants) return SetConversationAdminResult.MemberNotParticipant
        if (!command.isAdmin && bookingOwnerId(conversation) == command.memberId) {
            return SetConversationAdminResult.CannotDemoteBookingOwner
        }

        chatRepository.setParticipantAdmin(command.conversationId, command.memberId, command.isAdmin)

        return SetConversationAdminResult.Success(
            buildConversationDetail(conversation, command.currentUserId),
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
        val conversation =
            chatRepository.findConversationById(command.conversationId)
                ?: return ToggleReactionResult.ConversationNotFound
        if (!chatRepository.isParticipant(command.conversationId, command.currentUserId)) {
            return ToggleReactionResult.NotParticipant
        }
        if (isBookingChatClosed(conversation)) return ToggleReactionResult.ChatClosed
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
            title =
                conversation?.title?.takeIf { it.isNotBlank() }
                    ?: defaultTitle(conversation, participants, currentUserId, users, counterpart),
            canRename = canRename(conversation, participants, currentUserId),
            counterpartName = counterpart.displayName(),
            counterpartPhotoKey = counterpart?.photoKey,
            lastMessagePreview = lastMessage?.text?.take(PREVIEW_MAX_LENGTH) ?: "",
            lastMessageAt = lastMessage?.sentAt ?: conversation?.createdAt ?: Instant.now(),
            isLastFromMe = lastMessage?.senderId == currentUserId,
            unreadCount = chatRepository.countUnread(conversationId, currentUserId, lastReadAt),
        )
    }

    /**
     * Titre déduit quand aucun titre n'a été choisi : une conversation de réservation porte le nom de la
     * salle suivi du créneau exprimé dans le fuseau de la salle, un groupe porte les noms des autres
     * participants, une conversation à deux porte le nom de l'interlocuteur.
     */
    private fun defaultTitle(
        conversation: Conversation?,
        participants: Set<UserId>,
        currentUserId: UserId,
        users: (UserId) -> User?,
        counterpart: User?,
    ): String {
        val bookingTitle = conversation?.bookingId?.let(::bookingTitle)
        if (bookingTitle != null) return bookingTitle
        if (participants.size > 2) {
            val others =
                participants
                    .filter { it != currentUserId }
                    .map { users(it).displayName() }
                    .sorted()
            if (others.isNotEmpty()) return others.joinToString(", ")
        }
        return counterpart.displayName()
    }

    private fun bookingTitle(bookingId: BookingId): String? {
        val booking = bookingRepository.findById(bookingId) ?: return null
        val room = roomRepository.findById(booking.roomId) ?: return null
        val start = booking.startAt.atZone(room.timeZone)
        val end = booking.endAt.atZone(room.timeZone)
        return "${room.name} · ${TITLE_DAY_FORMAT.format(start)} " +
            "${TITLE_TIME_FORMAT.format(start)}-${TITLE_TIME_FORMAT.format(end)}"
    }

    private fun canRename(
        conversation: Conversation?,
        participants: Set<UserId>,
        currentUserId: UserId,
    ): Boolean = conversation != null && isGroupAdmin(conversation, participants, currentUserId)

    /**
     * Administrateur du groupe : le client à l'origine de la réservation l'est d'office, les autres le
     * deviennent par promotion. Une conversation à deux n'a pas d'administrateur.
     */
    private fun isGroupAdmin(
        conversation: Conversation,
        participants: Set<UserId>,
        currentUserId: UserId,
    ): Boolean {
        if (bookingOwnerId(conversation) == currentUserId) return true
        if (participants.size <= 2 && conversation.bookingId == null) return false
        return currentUserId in chatRepository.findAdminIds(conversation.id)
    }

    private fun bookingOwnerId(conversation: Conversation): UserId? = conversation.bookingId?.let { bookingRepository.findById(it)?.userId }

    private fun buildConversationDetail(
        conversation: Conversation,
        currentUserId: UserId,
    ): ConversationDetailView {
        val participants = chatRepository.findParticipantIds(conversation.id)
        val declaredAdmins = chatRepository.findAdminIds(conversation.id)
        val ownerId = bookingOwnerId(conversation)
        val users = resolver()
        val members =
            participants
                .mapNotNull { users(it) }
                .map { user ->
                    ConversationMemberView(
                        userId = user.id,
                        displayName = user.displayName(),
                        photoKey = user.photoKey,
                        role = user.role,
                        isAdmin = user.id == ownerId || user.id in declaredAdmins,
                        isMe = user.id == currentUserId,
                    )
                }.sortedWith(compareByDescending<ConversationMemberView> { it.isAdmin }.thenBy { it.displayName })
        val isAdmin = isGroupAdmin(conversation, participants, currentUserId)
        return ConversationDetailView(
            id = conversation.id,
            bookingId = conversation.bookingId,
            title =
                conversation.title?.takeIf { it.isNotBlank() }
                    ?: defaultTitle(
                        conversation,
                        participants,
                        currentUserId,
                        users,
                        users(participants.firstOrNull { it != currentUserId } ?: currentUserId),
                    ),
            isGroup = conversation.bookingId != null || participants.size > 2,
            canRename = isAdmin,
            isAdmin = isAdmin,
            members = members,
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

    /** Vrai si la conversation est rattachée à une réservation dont la fenêtre de chat (fin + 24 h) est échue. */
    private fun isBookingChatClosed(conversation: Conversation): Boolean {
        val bookingId = conversation.bookingId ?: return false
        val booking = bookingRepository.findById(bookingId) ?: return false
        return Instant.now().isAfter(booking.endAt.plus(Conversation.BOOKING_CHAT_WINDOW_AFTER_END))
    }
}

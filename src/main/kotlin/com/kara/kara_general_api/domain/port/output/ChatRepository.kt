package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.Message
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageReaction
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

/**
 * Port secondaire de persistance du chat. Aucune donnée utilisateur n'est dupliquée ici : seules
 * les références (identifiants) sont stockées, les noms/photos sont résolus via [UserRepository].
 */
interface ChatRepository {
    fun createConversation(
        conversation: Conversation,
        participantIds: Set<UserId>,
    )

    /** Conversation rattachée à une réservation, si elle existe déjà. */
    fun findConversationByBookingId(bookingId: BookingId): Conversation?

    /** Ajoute des participants à une conversation existante (idempotent : ignore ceux déjà présents). */
    fun addParticipants(
        conversationId: ConversationId,
        participantIds: Set<UserId>,
    )

    fun findConversationById(id: ConversationId): Conversation?

    /** Conversations où [userId] est participant, triées par activité récente décroissante. */
    fun findConversationsForUser(userId: UserId): List<Conversation>

    /** Toutes les conversations (supervision admin), triées par activité récente décroissante. */
    fun findAllConversations(): List<Conversation>

    /** Renvoie la conversation dont l'ensemble des participants est exactement [participantIds], si elle existe. */
    fun findConversationByExactParticipants(participantIds: Set<UserId>): Conversation?

    fun findParticipantIds(conversationId: ConversationId): Set<UserId>

    fun isParticipant(
        conversationId: ConversationId,
        userId: UserId,
    ): Boolean

    fun saveMessage(message: Message): Message

    fun findMessageById(id: MessageId): Message?

    /** Fenêtre paginée par curseur : les [limit] messages les plus récents antérieurs à [before], en ordre croissant. */
    fun findMessages(
        conversationId: ConversationId,
        limit: Int,
        before: Instant?,
    ): List<Message>

    fun findLastMessage(conversationId: ConversationId): Message?

    fun deleteMessage(id: MessageId)

    fun reactionExists(
        messageId: MessageId,
        userId: UserId,
        emoji: String,
    ): Boolean

    fun addReaction(reaction: MessageReaction)

    fun removeReaction(
        messageId: MessageId,
        userId: UserId,
        emoji: String,
    )

    fun findReactions(messageId: MessageId): List<MessageReaction>

    /** Positionne la date de dernière lecture de [userId] au plus tard entre l'actuelle et [at]. */
    fun markReadUpTo(
        conversationId: ConversationId,
        userId: UserId,
        at: Instant,
    )

    fun findLastReadAtByParticipant(conversationId: ConversationId): Map<UserId, Instant?>

    /** Nombre de messages postés par d'autres que [userId] après [since] (tous si [since] est null). */
    fun countUnread(
        conversationId: ConversationId,
        userId: UserId,
        since: Instant?,
    ): Int
}

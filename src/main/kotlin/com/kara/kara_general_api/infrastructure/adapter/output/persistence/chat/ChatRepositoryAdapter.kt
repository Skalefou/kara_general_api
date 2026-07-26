package com.kara.kara_general_api.infrastructure.adapter.output.persistence.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.chat.Conversation
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.Message
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageReaction
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ChatRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Component
class ChatRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : ChatRepository {
    private fun mapConversation(rs: ResultSet): Conversation =
        Conversation(
            id = ConversationId(rs.getObject("id", UUID::class.java)),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            bookingId = rs.getObject("booking_id", UUID::class.java)?.let { BookingId(it) },
        )

    private val messageRowMapper =
        RowMapper { rs, _ ->
            Message(
                id = MessageId(rs.getObject("id", UUID::class.java)),
                conversationId = ConversationId(rs.getObject("conversation_id", UUID::class.java)),
                senderId = UserId(rs.getObject("sender_id", UUID::class.java)),
                type = rs.getString("type"),
                text = rs.getString("text"),
                replyToId = rs.getObject("reply_to_id", UUID::class.java)?.let { MessageId(it) },
                isForwarded = rs.getBoolean("is_forwarded"),
                isPinned = rs.getBoolean("is_pinned"),
                sentAt = rs.getTimestamp("sent_at").toInstant(),
            )
        }

    override fun createConversation(
        conversation: Conversation,
        participantIds: Set<UserId>,
    ) {
        jdbc.update(
            "INSERT INTO conversations (id, created_at, booking_id) VALUES (:id, :createdAt, :bookingId)",
            MapSqlParameterSource()
                .addValue("id", conversation.id.value)
                .addValue("createdAt", Timestamp.from(conversation.createdAt))
                .addValue("bookingId", conversation.bookingId?.value),
        )
        addParticipants(conversation.id, participantIds)
    }

    override fun findConversationByBookingId(bookingId: BookingId): Conversation? {
        val sql = "SELECT id, created_at, booking_id FROM conversations WHERE booking_id = :bookingId"
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value)) { rs, _ -> mapConversation(rs) }.firstOrNull()
    }

    override fun addParticipants(
        conversationId: ConversationId,
        participantIds: Set<UserId>,
    ) {
        if (participantIds.isEmpty()) return
        val batch =
            participantIds
                .map { userId ->
                    MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("conversationId", conversationId.value)
                        .addValue("userId", userId.value)
                }.toTypedArray()
        jdbc.batchUpdate(
            """
            INSERT INTO conversation_participants (id, conversation_id, user_id, last_read_at, created_at)
            VALUES (:id, :conversationId, :userId, NULL, NOW())
            ON CONFLICT ON CONSTRAINT uq_conversation_participants_conversation_user DO NOTHING
            """.trimIndent(),
            batch,
        )
    }

    override fun findConversationById(id: ConversationId): Conversation? {
        val sql = "SELECT id, created_at, booking_id FROM conversations WHERE id = :id"
        return jdbc.query(sql, mapOf("id" to id.value)) { rs, _ -> mapConversation(rs) }.firstOrNull()
    }

    // Tri par activité récente : dernier message posté, à défaut date de création de la conversation.
    override fun findConversationsForUser(userId: UserId): List<Conversation> {
        val sql =
            """
            SELECT c.id, c.created_at, c.booking_id
            FROM conversations c
            JOIN conversation_participants p ON p.conversation_id = c.id AND p.user_id = :userId
            ORDER BY COALESCE(
                (SELECT MAX(m.sent_at) FROM messages m WHERE m.conversation_id = c.id),
                c.created_at
            ) DESC
            """.trimIndent()
        return jdbc.query(sql, mapOf("userId" to userId.value)) { rs, _ -> mapConversation(rs) }
    }

    override fun findAllConversations(): List<Conversation> {
        val sql =
            """
            SELECT c.id, c.created_at, c.booking_id
            FROM conversations c
            ORDER BY COALESCE(
                (SELECT MAX(m.sent_at) FROM messages m WHERE m.conversation_id = c.id),
                c.created_at
            ) DESC
            """.trimIndent()
        return jdbc.query(sql) { rs, _ -> mapConversation(rs) }
    }

    // Conversation dont l'ensemble des participants est EXACTEMENT celui fourni : même cardinalité et
    // tous les participants appartiennent à l'ensemble demandé.
    override fun findConversationByExactParticipants(participantIds: Set<UserId>): Conversation? {
        if (participantIds.isEmpty()) return null
        val ids = participantIds.map { it.value }
        val sql =
            """
            SELECT c.id, c.created_at, c.booking_id
            FROM conversations c
            WHERE c.id = (
                SELECT p.conversation_id
                FROM conversation_participants p
                GROUP BY p.conversation_id
                HAVING COUNT(*) = :count
                   AND COUNT(*) FILTER (WHERE p.user_id IN (:ids)) = :count
                LIMIT 1
            )
            """.trimIndent()
        val params =
            MapSqlParameterSource()
                .addValue("count", ids.size)
                .addValue("ids", ids)
        return jdbc.query(sql, params) { rs, _ -> mapConversation(rs) }.firstOrNull()
    }

    override fun findParticipantIds(conversationId: ConversationId): Set<UserId> {
        val sql = "SELECT user_id FROM conversation_participants WHERE conversation_id = :id"
        return jdbc
            .query(sql, mapOf("id" to conversationId.value)) { rs, _ ->
                UserId(rs.getObject("user_id", UUID::class.java))
            }.toSet()
    }

    override fun isParticipant(
        conversationId: ConversationId,
        userId: UserId,
    ): Boolean {
        val sql =
            """
            SELECT COUNT(*) FROM conversation_participants
            WHERE conversation_id = :conversationId AND user_id = :userId
            """.trimIndent()
        val count =
            jdbc.queryForObject(
                sql,
                mapOf("conversationId" to conversationId.value, "userId" to userId.value),
                Int::class.java,
            ) ?: 0
        return count > 0
    }

    override fun saveMessage(message: Message): Message {
        val sql =
            """
            INSERT INTO messages (id, conversation_id, sender_id, type, text, reply_to_id,
                                  is_forwarded, is_pinned, sent_at)
            VALUES (:id, :conversationId, :senderId, :type, :text, :replyToId,
                    :isForwarded, :isPinned, :sentAt)
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", message.id.value)
                .addValue("conversationId", message.conversationId.value)
                .addValue("senderId", message.senderId.value)
                .addValue("type", message.type)
                .addValue("text", message.text)
                .addValue("replyToId", message.replyToId?.value)
                .addValue("isForwarded", message.isForwarded)
                .addValue("isPinned", message.isPinned)
                .addValue("sentAt", Timestamp.from(message.sentAt)),
        )
        return message
    }

    override fun findMessageById(id: MessageId): Message? {
        val sql =
            """
            SELECT id, conversation_id, sender_id, type, text, reply_to_id, is_forwarded, is_pinned, sent_at
            FROM messages
            WHERE id = :id
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), messageRowMapper).firstOrNull()
    }

    // Fenêtre par curseur : on prend les [limit] messages les plus récents antérieurs à :before
    // (ordre décroissant), puis on renvoie en ordre croissant attendu par le front. Le prédicat de
    // curseur n'est ajouté que si :before est fourni (évite un paramètre NULL non typable par PostgreSQL).
    override fun findMessages(
        conversationId: ConversationId,
        limit: Int,
        before: Instant?,
    ): List<Message> {
        val params =
            MapSqlParameterSource()
                .addValue("conversationId", conversationId.value)
                .addValue("limit", limit)
        val cursor =
            if (before != null) {
                params.addValue("before", Timestamp.from(before))
                "AND sent_at < :before"
            } else {
                ""
            }
        val sql =
            """
            SELECT id, conversation_id, sender_id, type, text, reply_to_id, is_forwarded, is_pinned, sent_at
            FROM messages
            WHERE conversation_id = :conversationId
              $cursor
            ORDER BY sent_at DESC
            LIMIT :limit
            """.trimIndent()
        return jdbc.query(sql, params, messageRowMapper).reversed()
    }

    override fun findLastMessage(conversationId: ConversationId): Message? {
        val sql =
            """
            SELECT id, conversation_id, sender_id, type, text, reply_to_id, is_forwarded, is_pinned, sent_at
            FROM messages
            WHERE conversation_id = :conversationId
            ORDER BY sent_at DESC
            LIMIT 1
            """.trimIndent()
        return jdbc.query(sql, mapOf("conversationId" to conversationId.value), messageRowMapper).firstOrNull()
    }

    override fun deleteMessage(id: MessageId) {
        jdbc.update("DELETE FROM messages WHERE id = :id", mapOf("id" to id.value))
    }

    override fun reactionExists(
        messageId: MessageId,
        userId: UserId,
        emoji: String,
    ): Boolean {
        val sql =
            """
            SELECT COUNT(*) FROM message_reactions
            WHERE message_id = :messageId AND user_id = :userId AND emoji = :emoji
            """.trimIndent()
        val count =
            jdbc.queryForObject(
                sql,
                mapOf("messageId" to messageId.value, "userId" to userId.value, "emoji" to emoji),
                Int::class.java,
            ) ?: 0
        return count > 0
    }

    override fun addReaction(reaction: MessageReaction) {
        val sql =
            """
            INSERT INTO message_reactions (id, message_id, user_id, emoji, created_at)
            VALUES (:id, :messageId, :userId, :emoji, NOW())
            ON CONFLICT ON CONSTRAINT uq_message_reactions_message_user_emoji DO NOTHING
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("messageId", reaction.messageId.value)
                .addValue("userId", reaction.userId.value)
                .addValue("emoji", reaction.emoji),
        )
    }

    override fun removeReaction(
        messageId: MessageId,
        userId: UserId,
        emoji: String,
    ) {
        val sql =
            """
            DELETE FROM message_reactions
            WHERE message_id = :messageId AND user_id = :userId AND emoji = :emoji
            """.trimIndent()
        jdbc.update(
            sql,
            mapOf("messageId" to messageId.value, "userId" to userId.value, "emoji" to emoji),
        )
    }

    override fun findReactions(messageId: MessageId): List<MessageReaction> {
        val sql =
            """
            SELECT message_id, user_id, emoji
            FROM message_reactions
            WHERE message_id = :messageId
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("messageId" to messageId.value)) { rs, _ ->
            MessageReaction(
                messageId = MessageId(rs.getObject("message_id", UUID::class.java)),
                userId = UserId(rs.getObject("user_id", UUID::class.java)),
                emoji = rs.getString("emoji"),
            )
        }
    }

    // Positionne last_read_at au plus tard entre la valeur actuelle et :at (jamais en arrière).
    override fun markReadUpTo(
        conversationId: ConversationId,
        userId: UserId,
        at: Instant,
    ) {
        val sql =
            """
            UPDATE conversation_participants
            SET last_read_at = :at
            WHERE conversation_id = :conversationId
              AND user_id = :userId
              AND (last_read_at IS NULL OR last_read_at < :at)
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("at", Timestamp.from(at))
                .addValue("conversationId", conversationId.value)
                .addValue("userId", userId.value),
        )
    }

    override fun findLastReadAtByParticipant(conversationId: ConversationId): Map<UserId, Instant?> {
        val sql =
            """
            SELECT user_id, last_read_at
            FROM conversation_participants
            WHERE conversation_id = :conversationId
            """.trimIndent()
        return jdbc
            .query(sql, mapOf("conversationId" to conversationId.value)) { rs, _ ->
                UserId(rs.getObject("user_id", UUID::class.java)) to rs.getTimestamp("last_read_at")?.toInstant()
            }.toMap()
    }

    override fun countUnread(
        conversationId: ConversationId,
        userId: UserId,
        since: Instant?,
    ): Int {
        val params =
            MapSqlParameterSource()
                .addValue("conversationId", conversationId.value)
                .addValue("userId", userId.value)
        val cursor =
            if (since != null) {
                params.addValue("since", Timestamp.from(since))
                "AND sent_at > :since"
            } else {
                ""
            }
        val sql =
            """
            SELECT COUNT(*) FROM messages
            WHERE conversation_id = :conversationId
              AND sender_id <> :userId
              $cursor
            """.trimIndent()
        return jdbc.queryForObject(sql, params, Int::class.java) ?: 0
    }
}

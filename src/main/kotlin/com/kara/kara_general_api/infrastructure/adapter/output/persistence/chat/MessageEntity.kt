package com.kara.kara_general_api.infrastructure.adapter.output.persistence.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif. */
@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_messages_conversation_sent_at", columnList = "conversation_id, sent_at"),
    ],
)
class MessageEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    var conversationId: UUID,
    @Column(name = "sender_id", nullable = false, columnDefinition = "uuid")
    var senderId: UUID,
    @Column(name = "type", nullable = false, columnDefinition = "varchar(20)")
    var type: String = "text",
    @Column(name = "text", columnDefinition = "text")
    var text: String? = null,
    @Column(name = "reply_to_id", columnDefinition = "uuid")
    var replyToId: UUID? = null,
    @Column(name = "is_forwarded", nullable = false, columnDefinition = "boolean default false")
    var isForwarded: Boolean = false,
    @Column(name = "is_pinned", nullable = false, columnDefinition = "boolean default false")
    var isPinned: Boolean = false,
    @Column(name = "sent_at", nullable = false, columnDefinition = "timestamptz")
    var sentAt: Instant = Instant.now(),
)

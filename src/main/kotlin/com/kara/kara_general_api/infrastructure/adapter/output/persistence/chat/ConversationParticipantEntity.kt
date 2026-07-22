package com.kara.kara_general_api.infrastructure.adapter.output.persistence.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Participation d'un utilisateur à une conversation (table `conversation_participants`). Porte l'état
 * de lecture par participant (`last_read_at`) qui pilote le compteur de non-lus.
 *
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 */
@Entity
@Table(
    name = "conversation_participants",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_conversation_participants_conversation_user",
            columnNames = ["conversation_id", "user_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_conversation_participants_user_id", columnList = "user_id"),
        Index(name = "idx_conversation_participants_conversation_id", columnList = "conversation_id"),
    ],
)
class ConversationParticipantEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    var conversationId: UUID,
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "last_read_at", columnDefinition = "timestamptz")
    var lastReadAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

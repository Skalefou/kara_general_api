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
 * Réaction (emoji) posée par un utilisateur sur un message (table `message_reactions`). La contrainte
 * unique (message, utilisateur, emoji) garantit l'idempotence de la bascule.
 *
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 */
@Entity
@Table(
    name = "message_reactions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_message_reactions_message_user_emoji",
            columnNames = ["message_id", "user_id", "emoji"],
        ),
    ],
    indexes = [Index(name = "idx_message_reactions_message_id", columnList = "message_id")],
)
class MessageReactionEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "message_id", nullable = false, columnDefinition = "uuid")
    var messageId: UUID,
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "emoji", nullable = false, columnDefinition = "varchar(32)")
    var emoji: String,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

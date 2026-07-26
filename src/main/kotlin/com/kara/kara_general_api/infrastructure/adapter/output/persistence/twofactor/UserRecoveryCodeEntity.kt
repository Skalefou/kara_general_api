package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Rôle UNIQUE : permettre à Hibernate de générer le DDL en dev (et le schéma des tests Testcontainers).
 * Cette classe n'est JAMAIS instanciée dans le code applicatif — miroir exact de `user_recovery_codes`
 * dans `db/init.sql`. `code_hash` (bcrypt) n'est jamais logué.
 */
@Entity
@Table(
    name = "user_recovery_codes",
    indexes = [
        Index(name = "idx_user_recovery_codes_user_id", columnList = "user_id"),
    ],
)
class UserRecoveryCodeEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "code_hash", nullable = false, length = 255)
    var codeHash: String,
    @Column(name = "used_at", columnDefinition = "timestamptz")
    var usedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

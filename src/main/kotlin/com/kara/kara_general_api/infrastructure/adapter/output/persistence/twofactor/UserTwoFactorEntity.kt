package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Rôle UNIQUE : permettre à Hibernate de générer le DDL en dev (et le schéma des tests Testcontainers).
 * Cette classe n'est JAMAIS instanciée dans le code applicatif — miroir exact de `user_two_factor`
 * dans `db/init.sql`.
 */
@Entity
@Table(name = "user_two_factor")
class UserTwoFactorEntity(
    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: UUID = UUID.randomUUID(),
    @Column(name = "secret_cipher", nullable = false, length = 512)
    var secretCipher: String,
    @Column(name = "status", nullable = false, length = 20)
    var status: String,
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
    @Column(name = "activated_at", columnDefinition = "timestamptz")
    var activatedAt: Instant? = null,
    @Column(name = "last_used_step")
    var lastUsedStep: Long? = null,
)

package com.kara.kara_general_api.infrastructure.adapter.output.persistence.image

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Corrélation jobId → entité (salle/profil). Rôle unique : génération du DDL en dev. Écrite lors de l'enqueue,
 * lue au retour du worker pour router le résultat vers la bonne entité.
 */
@Entity
@Table(name = "image_jobs")
class ImageJobCorrelationEntity(
    @Id
    @Column(name = "job_id", columnDefinition = "uuid")
    var jobId: UUID = UUID.randomUUID(),
    // ROOM | PROFILE
    @Column(nullable = false, columnDefinition = "varchar(20)")
    var target: String,
    // roomId (ROOM) ou userId (PROFILE)
    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    var ownerId: UUID,
    @Column(name = "image_id", nullable = false, columnDefinition = "uuid")
    var imageId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

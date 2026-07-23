package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif. */
@Entity
@Table(name = "room_images")
class RoomImageEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    // Identité de l'image partagée avec le worker (préfixe de clé des variantes) et cible de la FK des variantes.
    @Column(name = "image_id", nullable = false, unique = true, columnDefinition = "uuid")
    var imageId: UUID = UUID.randomUUID(),
    // Clé de l'ORIGINAL privé (`rooms/{roomId}/originals/{imageId}.{ext}`).
    @Column(name = "object_key", nullable = false, columnDefinition = "varchar(512)")
    var objectKey: String,
    // PROCESSING | READY | FAILED
    @Column(nullable = false, columnDefinition = "varchar(20) default 'PROCESSING'")
    var status: String = "PROCESSING",
    @Column(name = "error_code", columnDefinition = "varchar(50)")
    var errorCode: String? = null,
    @Column(nullable = false, columnDefinition = "int")
    var position: Int = 0,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

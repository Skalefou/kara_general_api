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
    @Column(name = "object_key", nullable = false, columnDefinition = "varchar(512)")
    var objectKey: String,
    @Column(nullable = false, columnDefinition = "int")
    var position: Int = 0,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

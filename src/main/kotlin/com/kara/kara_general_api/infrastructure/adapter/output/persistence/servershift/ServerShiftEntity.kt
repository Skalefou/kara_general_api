package com.kara.kara_general_api.infrastructure.adapter.output.persistence.servershift

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
    name = "server_shifts",
    indexes = [
        Index(name = "idx_server_shifts_server_id", columnList = "server_id"),
        Index(name = "idx_server_shifts_room_id", columnList = "room_id"),
        Index(name = "idx_server_shifts_start_at", columnList = "start_at"),
    ],
)
class ServerShiftEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "server_id", nullable = false, columnDefinition = "uuid")
    var serverId: UUID,
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    var startAt: Instant,
    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    var endAt: Instant,
    @Column(columnDefinition = "text")
    var note: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif. */
@Entity
@Table(
    name = "room_options",
    indexes = [Index(name = "idx_room_options_room_id", columnList = "room_id")],
)
class RoomOptionEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    @Column(nullable = false, columnDefinition = "varchar(255)")
    var label: String,
    @Column(columnDefinition = "text")
    var description: String? = null,
    @Column(nullable = false, columnDefinition = "numeric(10,2)")
    var price: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

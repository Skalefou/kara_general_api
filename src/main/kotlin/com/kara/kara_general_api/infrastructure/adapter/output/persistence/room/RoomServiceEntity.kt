package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Liaison salle↔service (table `room_services`). Le prix/label/description vivent sur le catalogue
 * global `services` ; cette table ne porte que le rattachement d'un service à une salle.
 *
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 */
@Entity
@Table(
    name = "room_services",
    uniqueConstraints = [UniqueConstraint(name = "uq_room_services_room_service", columnNames = ["room_id", "service_id"])],
    indexes = [
        Index(name = "idx_room_services_room_id", columnList = "room_id"),
        Index(name = "idx_room_services_service_id", columnList = "service_id"),
    ],
)
class RoomServiceEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    @Column(name = "service_id", nullable = false, columnDefinition = "uuid")
    var serviceId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

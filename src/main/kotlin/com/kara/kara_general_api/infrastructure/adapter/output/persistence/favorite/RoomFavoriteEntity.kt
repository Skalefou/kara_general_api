package com.kara.kara_general_api.infrastructure.adapter.output.persistence.favorite

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Liaison utilisateur↔salle favorite (table `room_favorites`). Un couple (user_id, room_id) est unique :
 * l'ajout d'un favori déjà présent est un no-op.
 *
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 */
@Entity
@Table(
    name = "room_favorites",
    uniqueConstraints = [UniqueConstraint(name = "uq_room_favorites_user_room", columnNames = ["user_id", "room_id"])],
    indexes = [
        Index(name = "idx_room_favorites_user_id", columnList = "user_id"),
        Index(name = "idx_room_favorites_room_id", columnList = "room_id"),
    ],
)
class RoomFavoriteEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

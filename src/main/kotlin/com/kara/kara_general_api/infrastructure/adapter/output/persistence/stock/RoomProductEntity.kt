package com.kara.kara_general_api.infrastructure.adapter.output.persistence.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Stock par salle (table `room_products`) : rattache un produit du catalogue générique à une salle
 * avec une quantité. Le nom/prix/description vivent sur `products`.
 *
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 */
@Entity
@Table(
    name = "room_products",
    uniqueConstraints = [UniqueConstraint(name = "uq_room_products_room_product", columnNames = ["room_id", "product_id"])],
    indexes = [
        Index(name = "idx_room_products_room_id", columnList = "room_id"),
        Index(name = "idx_room_products_product_id", columnList = "product_id"),
    ],
)
class RoomProductEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID,
    @Column(nullable = false)
    var quantity: Int = 0,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

package com.kara.kara_general_api.infrastructure.adapter.output.persistence.order

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
    name = "orders",
    indexes = [
        Index(name = "idx_orders_booking_id", columnList = "booking_id"),
    ],
)
class OrderEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID,
    @Column(nullable = false, columnDefinition = "int")
    var quantity: Int,
    @Column(name = "unit_price", nullable = false, columnDefinition = "numeric(10,2)")
    var unitPrice: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(name = "total_price", nullable = false, columnDefinition = "numeric(10,2)")
    var totalPrice: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(30)")
    var status: String,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

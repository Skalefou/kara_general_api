package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

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
    name = "bookings",
    indexes = [
        Index(name = "idx_bookings_room_id", columnList = "room_id"),
        Index(name = "idx_bookings_user_id", columnList = "user_id"),
    ],
)
class BookingEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    var roomId: UUID,
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    var startAt: Instant,
    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    var endAt: Instant,
    @Column(name = "number_of_people", nullable = false, columnDefinition = "int")
    var numberOfPeople: Int,
    @Column(name = "total_price", nullable = false, columnDefinition = "numeric(10,2)")
    var totalPrice: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(nullable = false, columnDefinition = "varchar(50) default 'PENDING'")
    var status: String = "PENDING",
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

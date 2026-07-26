package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "booking_extensions",
    indexes = [
        Index(name = "idx_booking_extensions_booking_id", columnList = "booking_id"),
        Index(name = "idx_booking_extensions_user_id", columnList = "user_id"),
        Index(name = "idx_booking_extensions_status_expires", columnList = "status, expires_at"),
    ],
)
class BookingExtensionEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(name = "additional_minutes", nullable = false)
    var additionalMinutes: Int,
    @Column(name = "previous_end_at", nullable = false, columnDefinition = "timestamptz")
    var previousEndAt: Instant,
    @Column(name = "new_end_at", nullable = false, columnDefinition = "timestamptz")
    var newEndAt: Instant,
    @Column(nullable = false, columnDefinition = "numeric(10,2)")
    var price: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(nullable = false, columnDefinition = "varchar(50)")
    var status: String = "PENDING",
    @Column(name = "payment_mode", nullable = false, columnDefinition = "varchar(20)")
    var paymentMode: String = "PAY_ALL",
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz")
    var expiresAt: Instant,
)

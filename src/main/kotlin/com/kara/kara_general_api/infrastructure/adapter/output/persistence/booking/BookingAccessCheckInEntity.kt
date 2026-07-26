package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "booking_access_check_ins",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_booking_access_check_ins_booking", columnNames = ["booking_id"]),
    ],
    indexes = [
        Index(name = "idx_booking_access_check_ins_server_id", columnList = "server_id"),
    ],
)
class BookingAccessCheckInEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(name = "server_id", nullable = false, columnDefinition = "uuid")
    var serverId: UUID,
    @Column(name = "checked_in_at", nullable = false, columnDefinition = "timestamptz")
    var checkedInAt: Instant = Instant.now(),
)

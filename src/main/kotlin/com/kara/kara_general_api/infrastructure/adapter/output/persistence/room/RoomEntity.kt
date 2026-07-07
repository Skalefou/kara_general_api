package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "rooms")
class RoomEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "varchar(255)")
    var name: String,
    @Column(nullable = false, columnDefinition = "varchar(255)")
    var street: String,
    @Column(nullable = false, columnDefinition = "varchar(100)")
    var city: String,
    @Column(name = "postal_code", nullable = false, columnDefinition = "varchar(20)")
    var postalCode: String,
    @Column(nullable = false, columnDefinition = "varchar(100)")
    var country: String,
    @Column(nullable = false, columnDefinition = "varchar(50)")
    var status: String = "OPEN",
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

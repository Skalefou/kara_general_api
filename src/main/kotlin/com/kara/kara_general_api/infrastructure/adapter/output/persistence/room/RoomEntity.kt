package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

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
    name = "rooms",
    indexes = [Index(name = "idx_rooms_lat_lng", columnList = "latitude, longitude")],
)
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
    @Column(name = "price_per_person_per_hour", nullable = false, columnDefinition = "numeric(10,2)")
    var pricePerPersonPerHour: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(columnDefinition = "double precision")
    var latitude: Double? = null,
    @Column(columnDefinition = "double precision")
    var longitude: Double? = null,
    @Column(nullable = false, columnDefinition = "varchar(50)")
    var status: String = "OPEN",
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

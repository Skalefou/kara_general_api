package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime
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
    @Column(nullable = false, columnDefinition = "text default ''")
    var description: String,
    @Column(nullable = false, columnDefinition = "varchar(255)")
    var street: String,
    @Column(nullable = false, columnDefinition = "varchar(100)")
    var city: String,
    @Column(name = "postal_code", nullable = false, columnDefinition = "varchar(20)")
    var postalCode: String,
    @Column(nullable = false, columnDefinition = "varchar(100)")
    var country: String,
    @Column(name = "price_per_person_per_hour", nullable = false, columnDefinition = "numeric(10,2) default 0")
    var pricePerPersonPerHour: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10) default 'EUR'")
    var currency: String,
    @Column(name = "max_capacity", nullable = false, columnDefinition = "int default 0")
    var maxCapacity: Int,
    @Column(name = "is_there_wifi", nullable = false, columnDefinition = "boolean default false")
    var isThereWifi: Boolean,
    @Column(name = "is_there_sono_pro", nullable = false, columnDefinition = "boolean default false")
    var isThereSonoPro: Boolean,
    @Column(name = "is_there_air_conditioning", nullable = false, columnDefinition = "boolean default false")
    var isThereAirConditioning: Boolean,
    @Column(columnDefinition = "double precision")
    var latitude: Double? = null,
    @Column(columnDefinition = "double precision")
    var longitude: Double? = null,
    @Column(nullable = false, columnDefinition = "varchar(50)")
    var status: String = "OPEN",
    @Column(name = "opens_at", columnDefinition = "time")
    var opensAt: LocalTime? = null,
    @Column(name = "closes_at", columnDefinition = "time")
    var closesAt: LocalTime? = null,
    @Column(name = "time_zone", nullable = false, columnDefinition = "varchar(64) default 'Europe/Paris'")
    var timeZone: String = "Europe/Paris",
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Liaison réservation↔option (table `booking_options`), modelée sur `room_services`. Fige les
 * identifiants d'options retenus au moment de la réservation.
 *
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 */
@Entity
@Table(
    name = "booking_options",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_booking_options_booking_option", columnNames = ["booking_id", "option_id"]),
    ],
    indexes = [
        Index(name = "idx_booking_options_booking_id", columnList = "booking_id"),
        Index(name = "idx_booking_options_option_id", columnList = "option_id"),
    ],
)
class BookingOptionEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(name = "option_id", nullable = false, columnDefinition = "uuid")
    var optionId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

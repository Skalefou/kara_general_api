package com.kara.kara_general_api.infrastructure.adapter.output.persistence.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/**
 * Rôle unique : génération du DDL en dev (ddl-auto). Jamais instanciée dans le code applicatif.
 * Trace l'envoi d'un rappel de fin (`kind`) pour une réservation ; l'unicité (booking_id, kind)
 * garantit l'idempotence des rappels.
 */
@Entity
@Table(
    name = "booking_end_reminders",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_booking_end_reminders_booking_kind", columnNames = ["booking_id", "kind"]),
    ],
    indexes = [
        Index(name = "idx_booking_end_reminders_booking_id", columnList = "booking_id"),
    ],
)
class BookingEndReminderEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(nullable = false, columnDefinition = "varchar(20)")
    var kind: String,
    @Column(name = "sent_at", nullable = false, columnDefinition = "timestamptz")
    var sentAt: Instant = Instant.now(),
)

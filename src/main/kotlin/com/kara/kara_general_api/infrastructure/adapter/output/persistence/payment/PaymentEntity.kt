package com.kara.kara_general_api.infrastructure.adapter.output.persistence.payment

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
    name = "payments",
    indexes = [
        Index(name = "idx_payments_booking_id", columnList = "booking_id"),
        Index(name = "idx_payments_user_id", columnList = "user_id"),
    ],
)
class PaymentEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,
    @Column(nullable = false, columnDefinition = "numeric(10,2)")
    var amount: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(nullable = false, columnDefinition = "varchar(50) default 'PENDING'")
    var status: String = "PENDING",
    @Column(name = "stripe_payment_intent_id", nullable = false, unique = true, columnDefinition = "varchar(255)")
    var stripePaymentIntentId: String,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

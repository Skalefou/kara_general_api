package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

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
    name = "pools",
    indexes = [
        Index(name = "idx_pools_booking_id", columnList = "booking_id"),
        Index(name = "idx_pools_global_link_token", columnList = "global_link_token", unique = true),
        Index(name = "idx_pools_status_deadline", columnList = "status, deadline"),
    ],
)
class PoolEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "booking_id", nullable = false, columnDefinition = "uuid")
    var bookingId: UUID,
    @Column(name = "extension_id", columnDefinition = "uuid")
    var extensionId: UUID? = null,
    @Column(name = "target_amount", nullable = false, columnDefinition = "numeric(10,2)")
    var targetAmount: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(10)")
    var currency: String,
    @Column(nullable = false, columnDefinition = "varchar(50) default 'OPEN'")
    var status: String = "OPEN",
    @Column(nullable = false, columnDefinition = "timestamptz")
    var deadline: Instant,
    @Column(name = "global_link_token", nullable = false, unique = true, columnDefinition = "varchar(255)")
    var globalLinkToken: String,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

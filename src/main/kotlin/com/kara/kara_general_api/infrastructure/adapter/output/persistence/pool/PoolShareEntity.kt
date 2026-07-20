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
    name = "pool_shares",
    indexes = [
        Index(name = "idx_pool_shares_pool_id", columnList = "pool_id"),
        Index(name = "idx_pool_shares_unique_link_token", columnList = "unique_link_token", unique = true),
        Index(name = "idx_pool_shares_stripe_payment_intent_id", columnList = "stripe_payment_intent_id", unique = true),
    ],
)
class PoolShareEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "pool_id", nullable = false, columnDefinition = "uuid")
    var poolId: UUID,
    @Column(name = "participant_name", nullable = false, columnDefinition = "varchar(255)")
    var participantName: String,
    @Column(columnDefinition = "varchar(255)")
    var email: String? = null,
    @Column(nullable = false, columnDefinition = "numeric(10,2)")
    var amount: BigDecimal,
    @Column(nullable = false, columnDefinition = "varchar(50) default 'PENDING'")
    var status: String = "PENDING",
    @Column(name = "stripe_payment_intent_id", unique = true, columnDefinition = "varchar(255)")
    var stripePaymentIntentId: String? = null,
    @Column(name = "unique_link_token", unique = true, columnDefinition = "varchar(255)")
    var uniqueLinkToken: String? = null,
    @Column(name = "payer_user_id", columnDefinition = "uuid")
    var payerUserId: UUID? = null,
    @Column(name = "is_creator_share", nullable = false, columnDefinition = "boolean default false")
    var isCreatorShare: Boolean = false,
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: Instant = Instant.now(),
)

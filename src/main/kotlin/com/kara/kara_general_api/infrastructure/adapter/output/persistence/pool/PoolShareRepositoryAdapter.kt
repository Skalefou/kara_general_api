package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

private const val POOL_SHARE_COLUMNS =
    "id, pool_id, participant_name, email, amount, status, stripe_payment_intent_id, " +
        "unique_link_token, payer_user_id, is_creator_share"

@Component
class PoolShareRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: PoolShareRowMapper,
) : PoolShareRepository {

    override fun save(share: PoolShare): PoolShare {
        jdbc.update(UPSERT_SQL, params(share))
        return share
    }

    override fun saveAll(shares: List<PoolShare>): List<PoolShare> {
        if (shares.isEmpty()) return shares
        jdbc.batchUpdate(UPSERT_SQL, shares.map { params(it) }.toTypedArray())
        return shares
    }

    override fun findById(id: PoolShareId): PoolShare? {
        val sql = "SELECT $POOL_SHARE_COLUMNS FROM pool_shares WHERE id = :id"
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findByPoolId(poolId: PoolId): List<PoolShare> {
        val sql =
            """
            SELECT $POOL_SHARE_COLUMNS
            FROM pool_shares
            WHERE pool_id = :poolId
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("poolId" to poolId.value), rowMapper)
    }

    override fun findCreatorShareForUpdate(poolId: PoolId): PoolShare? {
        // Verrou pessimiste sur la ligne du reliquat créateur : sérialise les auto-inscriptions concurrentes.
        val sql =
            """
            SELECT $POOL_SHARE_COLUMNS
            FROM pool_shares
            WHERE pool_id = :poolId AND is_creator_share = TRUE
            FOR UPDATE
            """.trimIndent()
        return jdbc.query(sql, mapOf("poolId" to poolId.value), rowMapper).firstOrNull()
    }

    override fun findByUniqueLinkToken(token: String): PoolShare? {
        val sql = "SELECT $POOL_SHARE_COLUMNS FROM pool_shares WHERE unique_link_token = :token"
        return jdbc.query(sql, mapOf("token" to token), rowMapper).firstOrNull()
    }

    override fun findByStripePaymentIntentId(stripePaymentIntentId: String): PoolShare? {
        val sql = "SELECT $POOL_SHARE_COLUMNS FROM pool_shares WHERE stripe_payment_intent_id = :intentId"
        return jdbc.query(sql, mapOf("intentId" to stripePaymentIntentId), rowMapper).firstOrNull()
    }

    private fun params(share: PoolShare): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", share.id.value)
            .addValue("poolId", share.poolId.value)
            .addValue("participantName", share.participantName)
            .addValue("email", share.email?.value)
            .addValue("amount", share.amount)
            .addValue("status", share.status.name)
            .addValue("stripePaymentIntentId", share.stripePaymentIntentId)
            .addValue("uniqueLinkToken", share.uniqueLinkToken)
            .addValue("payerUserId", share.payerUserId?.value)
            .addValue("isCreatorShare", share.isCreatorShare)

    private companion object {
        val UPSERT_SQL =
            """
            INSERT INTO pool_shares (id, pool_id, participant_name, email, amount, status,
                                     stripe_payment_intent_id, unique_link_token, payer_user_id,
                                     is_creator_share, created_at)
            VALUES (:id, :poolId, :participantName, :email, :amount, :status,
                    :stripePaymentIntentId, :uniqueLinkToken, :payerUserId,
                    :isCreatorShare, NOW())
            ON CONFLICT (id) DO UPDATE SET
                participant_name         = EXCLUDED.participant_name,
                email                    = EXCLUDED.email,
                amount                   = EXCLUDED.amount,
                status                   = EXCLUDED.status,
                stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id,
                unique_link_token        = EXCLUDED.unique_link_token,
                payer_user_id            = EXCLUDED.payer_user_id,
                is_creator_share         = EXCLUDED.is_creator_share
            """.trimIndent()
    }
}

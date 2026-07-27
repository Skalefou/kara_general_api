package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.PoolRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant

private const val POOL_COLUMNS =
    "id, booking_id, extension_id, target_amount, currency, status, deadline, global_link_token, created_at"

private const val POOL_COLUMNS_P =
    "p.id, p.booking_id, p.extension_id, p.target_amount, p.currency, p.status, p.deadline, " +
        "p.global_link_token, p.created_at"

@Component
class PoolRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: PoolRowMapper,
) : PoolRepository {
    override fun save(pool: Pool): Pool {
        val sql =
            """
            INSERT INTO pools (id, booking_id, extension_id, target_amount, currency, status, deadline,
                               global_link_token, created_at)
            VALUES (:id, :bookingId, :extensionId, :targetAmount, :currency, :status, :deadline,
                    :globalLinkToken, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                status            = EXCLUDED.status,
                deadline          = EXCLUDED.deadline,
                global_link_token = EXCLUDED.global_link_token
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", pool.id.value)
                .addValue("bookingId", pool.bookingId.value)
                .addValue("extensionId", pool.extensionId?.value)
                .addValue("targetAmount", pool.targetAmount)
                .addValue("currency", pool.currency.name)
                .addValue("status", pool.status.name)
                .addValue("deadline", Timestamp.from(pool.deadline))
                .addValue("globalLinkToken", pool.globalLinkToken)
                .addValue("createdAt", Timestamp.from(pool.createdAt)),
        )
        return pool
    }

    override fun findById(id: PoolId): Pool? {
        val sql = "SELECT $POOL_COLUMNS FROM pools WHERE id = :id"
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findByBookingId(bookingId: BookingId): Pool? {
        val sql = "SELECT $POOL_COLUMNS FROM pools WHERE booking_id = :bookingId AND extension_id IS NULL"
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value), rowMapper).firstOrNull()
    }

    override fun findByExtensionId(extensionId: BookingExtensionId): Pool? {
        val sql = "SELECT $POOL_COLUMNS FROM pools WHERE extension_id = :extensionId"
        return jdbc.query(sql, mapOf("extensionId" to extensionId.value), rowMapper).firstOrNull()
    }

    override fun findByBookingIds(bookingIds: List<BookingId>): List<Pool> {
        if (bookingIds.isEmpty()) return emptyList()
        // Cagnottes de réservation uniquement (extension_id IS NULL) : les cagnottes d'extension ne
        // représentent pas le règlement initial de la réservation.
        val sql =
            """
            SELECT $POOL_COLUMNS
            FROM pools
            WHERE booking_id IN (:bookingIds)
              AND extension_id IS NULL
            """.trimIndent()
        val params = MapSqlParameterSource().addValue("bookingIds", bookingIds.map { it.value })
        return jdbc.query(sql, params, rowMapper)
    }

    override fun findByGlobalLinkToken(token: String): Pool? {
        val sql = "SELECT $POOL_COLUMNS FROM pools WHERE global_link_token = :token"
        return jdbc.query(sql, mapOf("token" to token), rowMapper).firstOrNull()
    }

    override fun updateStatus(
        id: PoolId,
        status: PoolStatus,
    ) {
        val sql = "UPDATE pools SET status = :status WHERE id = :id"
        jdbc.update(sql, mapOf("id" to id.value, "status" to status.name))
    }

    override fun updateGlobalLinkToken(
        id: PoolId,
        token: String,
    ) {
        val sql = "UPDATE pools SET global_link_token = :token WHERE id = :id"
        jdbc.update(sql, mapOf("id" to id.value, "token" to token))
    }

    override fun findExpiredOpen(now: Instant): List<Pool> {
        // Cagnottes OPEN dont le délai est échu : candidates à l'annulation (autorisations à lever).
        val sql =
            """
            SELECT $POOL_COLUMNS
            FROM pools
            WHERE status = 'OPEN'
              AND deadline <= :now
            ORDER BY deadline ASC
            """.trimIndent()
        return jdbc.query(sql, MapSqlParameterSource().addValue("now", Timestamp.from(now)), rowMapper)
    }

    override fun findByUserInvolvement(userId: UserId): List<Pool> {
        // Cagnottes de l'utilisateur : créateur (bookings.user_id) OU détenteur d'une part
        // (pool_shares.payer_user_id). DISTINCT car un utilisateur peut détenir plusieurs parts d'une même
        // cagnotte. Tri par échéance puis création décroissantes (« Mes événements »).
        val sql =
            """
            SELECT DISTINCT $POOL_COLUMNS_P
            FROM pools p
            JOIN bookings b ON b.id = p.booking_id
            LEFT JOIN pool_shares s ON s.pool_id = p.id
            WHERE b.user_id = :userId
               OR s.payer_user_id = :userId
            ORDER BY p.deadline DESC, p.created_at DESC
            """.trimIndent()
        return jdbc.query(sql, mapOf("userId" to userId.value), rowMapper)
    }
}

package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant

private const val COLUMNS =
    "id, booking_id, user_id, additional_minutes, previous_end_at, new_end_at, price, currency, " +
        "status, payment_mode, created_at, expires_at"

@Component
class BookingExtensionRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: BookingExtensionRowMapper,
) : BookingExtensionRepository {

    override fun save(extension: BookingExtension): BookingExtension {
        val sql =
            """
            INSERT INTO booking_extensions (id, booking_id, user_id, additional_minutes, previous_end_at,
                                            new_end_at, price, currency, status, payment_mode, created_at,
                                            expires_at)
            VALUES (:id, :bookingId, :userId, :additionalMinutes, :previousEndAt, :newEndAt, :price,
                    :currency, :status, :paymentMode, :createdAt, :expiresAt)
            ON CONFLICT (id) DO UPDATE SET
                status     = EXCLUDED.status,
                new_end_at = EXCLUDED.new_end_at,
                price      = EXCLUDED.price,
                expires_at = EXCLUDED.expires_at
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", extension.id.value)
                .addValue("bookingId", extension.bookingId.value)
                .addValue("userId", extension.userId.value)
                .addValue("additionalMinutes", extension.additionalMinutes)
                .addValue("previousEndAt", Timestamp.from(extension.previousEndAt))
                .addValue("newEndAt", Timestamp.from(extension.newEndAt))
                .addValue("price", extension.price)
                .addValue("currency", extension.currency.name)
                .addValue("status", extension.status.name)
                .addValue("paymentMode", extension.paymentMode.name)
                .addValue("createdAt", Timestamp.from(extension.createdAt))
                .addValue("expiresAt", Timestamp.from(extension.expiresAt)),
        )
        return extension
    }

    override fun findById(id: BookingExtensionId): BookingExtension? {
        val sql = "SELECT $COLUMNS FROM booking_extensions WHERE id = :id"
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findPendingByBookingId(bookingId: BookingId): BookingExtension? {
        val sql =
            "SELECT $COLUMNS FROM booking_extensions WHERE booking_id = :bookingId AND status = 'PENDING'"
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value), rowMapper).firstOrNull()
    }

    override fun updateStatus(id: BookingExtensionId, status: BookingExtensionStatus) {
        val sql = "UPDATE booking_extensions SET status = :status WHERE id = :id"
        jdbc.update(sql, mapOf("id" to id.value, "status" to status.name))
    }

    override fun findNextHeldStart(
        roomId: RoomId,
        after: Instant,
        excluding: BookingId,
        now: Instant,
    ): Instant? {
        val sql =
            """
            SELECT MIN(e.previous_end_at) AS next_start
            FROM booking_extensions e
            JOIN bookings b ON b.id = e.booking_id
            WHERE b.room_id = :roomId
              AND e.booking_id <> :excluding
              AND e.previous_end_at >= :after
              AND e.status = 'PENDING'
              AND e.expires_at > :now
            """.trimIndent()
        return jdbc.query(
            sql,
            MapSqlParameterSource()
                .addValue("roomId", roomId.value)
                .addValue("excluding", excluding.value)
                .addValue("after", Timestamp.from(after))
                .addValue("now", Timestamp.from(now)),
        ) { rs, _ -> rs.getTimestamp("next_start")?.toInstant() }
            .firstOrNull()
    }

    override fun findExpiredPending(now: Instant): List<BookingExtension> {
        val sql =
            "SELECT $COLUMNS FROM booking_extensions WHERE status = 'PENDING' AND expires_at <= :now"
        return jdbc.query(sql, mapOf("now" to Timestamp.from(now)), rowMapper)
    }
}

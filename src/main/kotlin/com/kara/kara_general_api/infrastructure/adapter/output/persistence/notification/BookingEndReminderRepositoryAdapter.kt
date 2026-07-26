package com.kara.kara_general_api.infrastructure.adapter.output.persistence.notification

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderKind
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderTarget
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingEndReminderRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Component
class BookingEndReminderRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : BookingEndReminderRepository {
    /**
     * Réservations CONFIRMED dont la fin tombe dans ]from, to] et sans rappel [kind] déjà enregistré
     * (NOT EXISTS). Jointure users → token FCM du client, jointure rooms → nom de la salle affiché.
     */
    override fun findConfirmedDue(
        kind: BookingEndReminderKind,
        from: Instant,
        to: Instant,
    ): List<BookingEndReminderTarget> {
        val sql =
            """
            SELECT b.id AS booking_id, b.user_id AS user_id, b.end_at AS end_at,
                   u.fcm_token AS fcm_token, r.name AS room_name
            FROM bookings b
            JOIN users u ON u.id = b.user_id
            JOIN rooms r ON r.id = b.room_id
            WHERE b.status = 'CONFIRMED'
              AND b.end_at > :from
              AND b.end_at <= :to
              AND NOT EXISTS (
                  SELECT 1 FROM booking_end_reminders ber
                  WHERE ber.booking_id = b.id
                    AND ber.kind = :kind
              )
            """.trimIndent()
        return jdbc.query(
            sql,
            MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to))
                .addValue("kind", kind.name),
        ) { rs, _ ->
            BookingEndReminderTarget(
                bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
                userId = UserId(rs.getObject("user_id", UUID::class.java)),
                fcmToken = rs.getString("fcm_token"),
                roomName = rs.getString("room_name"),
                endAt = rs.getTimestamp("end_at").toInstant(),
            )
        }
    }

    override fun markSent(
        bookingId: BookingId,
        kind: BookingEndReminderKind,
    ) {
        val sql =
            """
            INSERT INTO booking_end_reminders (id, booking_id, kind, sent_at)
            VALUES (:id, :bookingId, :kind, :sentAt)
            ON CONFLICT (booking_id, kind) DO NOTHING
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("bookingId", bookingId.value)
                .addValue("kind", kind.name)
                .addValue("sentAt", Timestamp.from(Instant.now())),
        )
    }

    override fun deleteByBookingId(bookingId: BookingId) {
        val sql = "DELETE FROM booking_end_reminders WHERE booking_id = :bookingId"
        jdbc.update(sql, mapOf("bookingId" to bookingId.value))
    }
}

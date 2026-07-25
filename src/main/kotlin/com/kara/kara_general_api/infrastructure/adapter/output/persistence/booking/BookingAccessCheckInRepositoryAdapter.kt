package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckIn
import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckInId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingAccessCheckInRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class BookingAccessCheckInRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : BookingAccessCheckInRepository {

    override fun findByBookingId(bookingId: BookingId): BookingAccessCheckIn? {
        val sql =
            """
            SELECT id, booking_id, server_id, checked_in_at
            FROM booking_access_check_ins
            WHERE booking_id = :bookingId
            """.trimIndent()
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value)) { rs, _ -> mapRow(rs) }
            .firstOrNull()
    }

    private fun mapRow(rs: ResultSet): BookingAccessCheckIn =
        BookingAccessCheckIn(
            id = BookingAccessCheckInId(rs.getObject("id", UUID::class.java)),
            bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
            serverId = UserId(rs.getObject("server_id", UUID::class.java)),
            checkedInAt = rs.getTimestamp("checked_in_at").toInstant(),
        )

    override fun recordIfAbsent(checkIn: BookingAccessCheckIn): BookingAccessCheckIn {
        val sql =
            """
            INSERT INTO booking_access_check_ins (id, booking_id, server_id, checked_in_at)
            VALUES (:id, :bookingId, :serverId, :checkedInAt)
            ON CONFLICT ON CONSTRAINT uq_booking_access_check_ins_booking
            DO UPDATE SET booking_id = booking_access_check_ins.booking_id
            RETURNING id, booking_id, server_id, checked_in_at
            """.trimIndent()
        return jdbc.query(
            sql,
            MapSqlParameterSource()
                .addValue("id", checkIn.id.value)
                .addValue("bookingId", checkIn.bookingId.value)
                .addValue("serverId", checkIn.serverId.value)
                .addValue("checkedInAt", java.sql.Timestamp.from(checkIn.checkedInAt)),
        ) { rs, _ -> mapRow(rs) }.first()
    }
}

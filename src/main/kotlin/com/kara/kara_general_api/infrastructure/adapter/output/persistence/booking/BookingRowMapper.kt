package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

/**
 * Mappe une ligne de `bookings` vers un [Booking]. Les options sélectionnées ne sont pas jointes ici :
 * l'adaptateur les charge séparément depuis `booking_options` et complète l'agrégat.
 */
@Component
class BookingRowMapper : RowMapper<Booking> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): Booking =
        Booking(
            id = BookingId(rs.getObject("id", UUID::class.java)),
            roomId = RoomId(rs.getObject("room_id", UUID::class.java)),
            userId = UserId(rs.getObject("user_id", UUID::class.java)),
            startAt = rs.getTimestamp("start_at").toInstant(),
            endAt = rs.getTimestamp("end_at").toInstant(),
            numberOfPeople = rs.getInt("number_of_people"),
            selectedOptionIds = emptyList(),
            totalPrice = rs.getBigDecimal("total_price"),
            currency = Currency.valueOf(rs.getString("currency")),
            status = BookingStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            paymentMode = PaymentMode.valueOf(rs.getString("payment_mode")),
        )
}

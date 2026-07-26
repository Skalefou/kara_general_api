package com.kara.kara_general_api.infrastructure.adapter.output.persistence.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class BookingExtensionRowMapper : RowMapper<BookingExtension> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): BookingExtension =
        BookingExtension(
            id = BookingExtensionId(rs.getObject("id", UUID::class.java)),
            bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
            userId = UserId(rs.getObject("user_id", UUID::class.java)),
            additionalMinutes = rs.getInt("additional_minutes"),
            previousEndAt = rs.getTimestamp("previous_end_at").toInstant(),
            newEndAt = rs.getTimestamp("new_end_at").toInstant(),
            price = rs.getBigDecimal("price"),
            currency = Currency.valueOf(rs.getString("currency")),
            status = BookingExtensionStatus.valueOf(rs.getString("status")),
            paymentMode = PaymentMode.valueOf(rs.getString("payment_mode")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
        )
}

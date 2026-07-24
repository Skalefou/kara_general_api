package com.kara.kara_general_api.infrastructure.adapter.output.persistence.payment

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class PaymentRowMapper : RowMapper<Payment> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Payment =
        Payment(
            id = PaymentId(rs.getObject("id", UUID::class.java)),
            bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
            userId = UserId(rs.getObject("user_id", UUID::class.java)),
            amount = rs.getBigDecimal("amount"),
            currency = Currency.valueOf(rs.getString("currency")),
            status = PaymentStatus.valueOf(rs.getString("status")),
            stripePaymentIntentId = rs.getString("stripe_payment_intent_id"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            extensionId =
                rs.getObject("extension_id", UUID::class.java)?.let { BookingExtensionId(it) },
        )
}

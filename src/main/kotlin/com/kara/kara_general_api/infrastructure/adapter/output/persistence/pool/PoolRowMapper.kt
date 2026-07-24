package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class PoolRowMapper : RowMapper<Pool> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Pool =
        Pool(
            id = PoolId(rs.getObject("id", UUID::class.java)),
            bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
            targetAmount = rs.getBigDecimal("target_amount"),
            currency = Currency.valueOf(rs.getString("currency")),
            status = PoolStatus.valueOf(rs.getString("status")),
            deadline = rs.getTimestamp("deadline").toInstant(),
            globalLinkToken = rs.getString("global_link_token"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}

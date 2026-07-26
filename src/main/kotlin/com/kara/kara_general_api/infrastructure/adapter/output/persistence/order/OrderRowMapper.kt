package com.kara.kara_general_api.infrastructure.adapter.output.persistence.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.model.order.OrderId
import com.kara.kara_general_api.domain.model.order.OrderStatus
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class OrderRowMapper : RowMapper<Order> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): Order =
        Order(
            id = OrderId(rs.getObject("id", UUID::class.java)),
            bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
            userId = UserId(rs.getObject("user_id", UUID::class.java)),
            productId = ProductId(rs.getObject("product_id", UUID::class.java)),
            quantity = rs.getInt("quantity"),
            unitPrice = rs.getBigDecimal("unit_price"),
            currency = Currency.valueOf(rs.getString("currency")),
            totalPrice = rs.getBigDecimal("total_price"),
            status = OrderStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}

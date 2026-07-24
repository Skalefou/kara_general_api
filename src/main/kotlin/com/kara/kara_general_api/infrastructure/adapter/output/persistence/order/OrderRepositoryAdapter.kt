package com.kara.kara_general_api.infrastructure.adapter.output.persistence.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.port.output.OrderRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class OrderRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: OrderRowMapper,
) : OrderRepository {

    override fun save(order: Order): Order {
        val sql =
            """
            INSERT INTO orders (id, booking_id, user_id, product_id, quantity, unit_price, currency,
                                total_price, status, created_at)
            VALUES (:id, :bookingId, :userId, :productId, :quantity, :unitPrice, :currency,
                    :totalPrice, :status, :createdAt)
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", order.id.value)
                .addValue("bookingId", order.bookingId.value)
                .addValue("userId", order.userId.value)
                .addValue("productId", order.productId.value)
                .addValue("quantity", order.quantity)
                .addValue("unitPrice", order.unitPrice)
                .addValue("currency", order.currency.name)
                .addValue("totalPrice", order.totalPrice)
                .addValue("status", order.status.name)
                .addValue("createdAt", Timestamp.from(order.createdAt)),
        )
        return order
    }

    override fun findByBookingId(bookingId: BookingId): List<Order> {
        val sql =
            """
            SELECT id, booking_id, user_id, product_id, quantity, unit_price, currency,
                   total_price, status, created_at
            FROM orders
            WHERE booking_id = :bookingId
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value), rowMapper)
    }
}

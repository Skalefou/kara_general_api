package com.kara.kara_general_api.infrastructure.adapter.output.persistence.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

private const val PAYMENT_COLUMNS =
    "id, booking_id, extension_id, user_id, amount, currency, status, stripe_payment_intent_id, created_at"

@Component
class PaymentRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: PaymentRowMapper,
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        val sql =
            """
            INSERT INTO payments (id, booking_id, extension_id, user_id, amount, currency, status,
                                  stripe_payment_intent_id, created_at)
            VALUES (:id, :bookingId, :extensionId, :userId, :amount, :currency, :status,
                    :stripePaymentIntentId, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                status = EXCLUDED.status
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", payment.id.value)
                .addValue("bookingId", payment.bookingId.value)
                .addValue("extensionId", payment.extensionId?.value)
                .addValue("userId", payment.userId.value)
                .addValue("amount", payment.amount)
                .addValue("currency", payment.currency.name)
                .addValue("status", payment.status.name)
                .addValue("stripePaymentIntentId", payment.stripePaymentIntentId)
                .addValue("createdAt", Timestamp.from(payment.createdAt)),
        )
        return payment
    }

    override fun findById(id: PaymentId): Payment? {
        val sql =
            """
            SELECT $PAYMENT_COLUMNS
            FROM payments
            WHERE id = :id
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findByStripePaymentIntentId(stripePaymentIntentId: String): Payment? {
        val sql =
            """
            SELECT $PAYMENT_COLUMNS
            FROM payments
            WHERE stripe_payment_intent_id = :stripePaymentIntentId
            """.trimIndent()
        return jdbc.query(
            sql,
            mapOf("stripePaymentIntentId" to stripePaymentIntentId),
            rowMapper,
        ).firstOrNull()
    }

    override fun findByBookingId(bookingId: BookingId): List<Payment> {
        val sql =
            """
            SELECT $PAYMENT_COLUMNS
            FROM payments
            WHERE booking_id = :bookingId
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("bookingId" to bookingId.value), rowMapper)
    }
}

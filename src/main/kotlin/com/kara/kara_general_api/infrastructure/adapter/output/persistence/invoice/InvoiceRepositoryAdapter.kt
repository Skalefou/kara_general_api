package com.kara.kara_general_api.infrastructure.adapter.output.persistence.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.InvoiceBuyer
import com.kara.kara_general_api.domain.port.output.InvoiceDetail
import com.kara.kara_general_api.domain.port.output.InvoiceRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InvoiceRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: InvoiceRowMapper,
) : InvoiceRepository {
    /**
     * Union des reçus du client : paiements « payer tout » PAID (source 'PAY') + parts de cagnotte CAPTURED
     * dont il est le payeur (source 'SHR'). Chaque source est jointe booking → room pour le libellé (nom de
     * salle). La devise d'une part vient de la cagnotte (`pools.currency`, absente de `pool_shares`).
     */
    override fun findByUser(userId: UserId): List<Invoice> {
        val sql =
            """
            SELECT 'PAY' AS source_type, p.id AS source_id, p.amount, p.currency,
                   p.created_at, p.booking_id, r.name AS room_name
            FROM payments p
            JOIN bookings b ON b.id = p.booking_id
            JOIN rooms r ON r.id = b.room_id
            WHERE p.user_id = :userId AND p.status = 'PAID'
            UNION ALL
            SELECT 'SHR' AS source_type, ps.id AS source_id, ps.amount, po.currency,
                   ps.created_at, po.booking_id, r.name AS room_name
            FROM pool_shares ps
            JOIN pools po ON po.id = ps.pool_id
            JOIN bookings b ON b.id = po.booking_id
            JOIN rooms r ON r.id = b.room_id
            WHERE ps.payer_user_id = :userId AND ps.status = 'CAPTURED'
            ORDER BY created_at DESC
            """.trimIndent()
        return jdbc.query(sql, mapOf("userId" to userId.value), rowMapper)
    }

    /** Reçu + acheteur d'un paiement « payer tout », uniquement s'il est PAID (le filtre statut fait foi). */
    override fun findReservationDetail(paymentId: PaymentId): InvoiceDetail? {
        val sql =
            """
            SELECT p.id AS source_id, p.amount, p.currency, p.created_at, p.booking_id,
                   r.name AS room_name, p.user_id, u.first_name, u.last_name, u.email
            FROM payments p
            JOIN bookings b ON b.id = p.booking_id
            JOIN rooms r ON r.id = b.room_id
            JOIN users u ON u.id = p.user_id
            WHERE p.id = :paymentId AND p.status = 'PAID'
            """.trimIndent()
        return jdbc
            .query(sql, mapOf("paymentId" to paymentId.value)) { rs, _ ->
                detail(
                    invoiceId = InvoiceId.reservation(PaymentId(rs.getObject("source_id", UUID::class.java))),
                    type = InvoiceType.RESERVATION,
                    rs = rs,
                    ownerColumn = "user_id",
                )
            }.firstOrNull()
    }

    /** Reçu + acheteur d'une part de cagnotte, uniquement si elle est CAPTURED. */
    override fun findCagnotteDetail(shareId: PoolShareId): InvoiceDetail? {
        val sql =
            """
            SELECT ps.id AS source_id, ps.amount, po.currency, ps.created_at, po.booking_id AS booking_id,
                   r.name AS room_name, ps.payer_user_id AS user_id, u.first_name, u.last_name, u.email
            FROM pool_shares ps
            JOIN pools po ON po.id = ps.pool_id
            JOIN bookings b ON b.id = po.booking_id
            JOIN rooms r ON r.id = b.room_id
            JOIN users u ON u.id = ps.payer_user_id
            WHERE ps.id = :shareId AND ps.status = 'CAPTURED'
            """.trimIndent()
        return jdbc
            .query(sql, mapOf("shareId" to shareId.value)) { rs, _ ->
                detail(
                    invoiceId = InvoiceId.cagnotte(PoolShareId(rs.getObject("source_id", UUID::class.java))),
                    type = InvoiceType.CAGNOTTE,
                    rs = rs,
                    ownerColumn = "user_id",
                )
            }.firstOrNull()
    }

    private fun detail(
        invoiceId: InvoiceId,
        type: InvoiceType,
        rs: java.sql.ResultSet,
        ownerColumn: String,
    ): InvoiceDetail {
        val invoice =
            Invoice(
                id = invoiceId,
                type = type,
                label = rs.getString("room_name"),
                amount = rs.getBigDecimal("amount"),
                currency = Currency.valueOf(rs.getString("currency")),
                issuedAt = rs.getTimestamp("created_at").toInstant(),
                bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
            )
        val fullName = "${rs.getString("first_name")} ${rs.getString("last_name")}".trim()
        return InvoiceDetail(
            invoice = invoice,
            ownerId = UserId(rs.getObject(ownerColumn, UUID::class.java)),
            buyer = InvoiceBuyer(fullName = fullName, email = rs.getString("email")),
        )
    }
}

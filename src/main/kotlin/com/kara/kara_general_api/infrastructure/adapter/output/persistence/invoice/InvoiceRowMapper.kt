package com.kara.kara_general_api.infrastructure.adapter.output.persistence.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.room.Currency
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

/**
 * Mappe une ligne de reçu dérivée. La colonne `source_type` ('PAY' | 'SHR') détermine le type de reçu et
 * la construction de l'identifiant opaque (`PAY-<paymentId>` / `SHR-<shareId>`).
 */
@Component
class InvoiceRowMapper : RowMapper<Invoice> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Invoice {
        val sourceType = rs.getString("source_type")
        val sourceId = rs.getObject("source_id", UUID::class.java)
        val (type, invoiceId) =
            if (sourceType == "PAY") {
                InvoiceType.RESERVATION to InvoiceId.reservation(PaymentId(sourceId))
            } else {
                InvoiceType.CAGNOTTE to InvoiceId.cagnotte(PoolShareId(sourceId))
            }
        return Invoice(
            id = invoiceId,
            type = type,
            label = rs.getString("room_name"),
            amount = rs.getBigDecimal("amount"),
            currency = Currency.valueOf(rs.getString("currency")),
            issuedAt = rs.getTimestamp("created_at").toInstant(),
            bookingId = BookingId(rs.getObject("booking_id", UUID::class.java)),
        )
    }
}

package com.kara.kara_general_api.domain.model.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.room.Currency
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvoiceTest {
    private fun invoice(
        id: InvoiceId,
        issuedAt: Instant,
    ) = Invoice(
        id = id,
        type = InvoiceType.RESERVATION,
        label = "Salle Étoile",
        amount = BigDecimal("435.00"),
        currency = Currency.EUR,
        issuedAt = issuedAt,
        bookingId = BookingId(UUID.randomUUID()),
    )

    @Test
    fun `invoice number is deterministic for a given source and year`() {
        val id = InvoiceId.reservation(PaymentId(UUID.randomUUID()))
        val at = Instant.parse("2026-03-04T10:00:00Z")

        assertEquals(invoice(id, at).number(), invoice(id, at).number())
    }

    @Test
    fun `invoice number embeds the issuing year and 8 Crockford characters`() {
        val number = invoice(InvoiceId.reservation(PaymentId(UUID.randomUUID())), Instant.parse("2026-07-22T00:00:00Z")).number()

        assertTrue(number.startsWith("INV-2026-"), "unexpected number: $number")
        val suffix = number.removePrefix("INV-2026-")
        assertEquals(8, suffix.length)
        assertTrue(suffix.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" }, "unexpected suffix: $suffix")
    }

    @Test
    fun `parse resolves a reservation id`() {
        val paymentId = PaymentId(UUID.randomUUID())

        val source = assertInstanceOf<InvoiceSource.Reservation>(InvoiceId.parse("PAY-${paymentId.value}"))
        assertEquals(paymentId, source.paymentId)
    }

    @Test
    fun `parse resolves a cagnotte id`() {
        val shareId = PoolShareId(UUID.randomUUID())

        val source = assertInstanceOf<InvoiceSource.Cagnotte>(InvoiceId.parse("SHR-${shareId.value}"))
        assertEquals(shareId, source.shareId)
    }

    @Test
    fun `parse returns null for an unknown prefix or malformed uuid`() {
        assertNull(InvoiceId.parse("XXX-${UUID.randomUUID()}"))
        assertNull(InvoiceId.parse("PAY-not-a-uuid"))
        assertNull(InvoiceId.parse("garbage"))
    }

    @Test
    fun `sourceUuid extracts the underlying uuid`() {
        val uuid = UUID.randomUUID()

        assertEquals(uuid, InvoiceId.reservation(PaymentId(uuid)).sourceUuid())
        assertEquals(uuid, InvoiceId.cagnotte(PoolShareId(uuid)).sourceUuid())
    }
}

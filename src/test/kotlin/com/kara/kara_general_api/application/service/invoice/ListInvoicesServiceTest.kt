package com.kara.kara_general_api.application.service.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.InvoiceRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ListInvoicesServiceTest {

    private val invoiceRepository = mockk<InvoiceRepository>()
    private val sut = ListInvoicesService(invoiceRepository)

    @Test
    fun `returns the union of receipts for the user`() {
        val userId = UserId(UUID.randomUUID())
        val reservation =
            Invoice(
                InvoiceId.reservation(PaymentId(UUID.randomUUID())), InvoiceType.RESERVATION, "Salle A",
                BigDecimal("100.00"), Currency.EUR, Instant.parse("2026-07-10T10:00:00Z"), BookingId(UUID.randomUUID()),
            )
        val cagnotte =
            Invoice(
                InvoiceId.cagnotte(PoolShareId(UUID.randomUUID())), InvoiceType.CAGNOTTE, "Salle B",
                BigDecimal("50.00"), Currency.EUR, Instant.parse("2026-07-05T10:00:00Z"), BookingId(UUID.randomUUID()),
            )
        every { invoiceRepository.findByUser(userId) } returns listOf(reservation, cagnotte)

        assertEquals(listOf(reservation, cagnotte), sut.listInvoices(userId))
    }

    @Test
    fun `returns an empty list when the user has no receipt`() {
        val userId = UserId(UUID.randomUUID())
        every { invoiceRepository.findByUser(userId) } returns emptyList()

        assertEquals(emptyList(), sut.listInvoices(userId))
    }
}

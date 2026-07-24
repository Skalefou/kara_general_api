package com.kara.kara_general_api.application.service.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadResult
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.InvoiceBuyer
import com.kara.kara_general_api.domain.port.output.InvoiceDetail
import com.kara.kara_general_api.domain.port.output.InvoicePdfGenerator
import com.kara.kara_general_api.domain.port.output.InvoiceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class GetInvoiceDownloadServiceTest {

    private val invoiceRepository = mockk<InvoiceRepository>()
    private val pdfGenerator = mockk<InvoicePdfGenerator>(relaxed = true)
    private val objectStorage = mockk<ImageStoragePort>(relaxed = true)
    private val sut = GetInvoiceDownloadService(invoiceRepository, pdfGenerator, objectStorage)

    private val ownerId = UserId(UUID.randomUUID())
    private val paymentId = PaymentId(UUID.randomUUID())

    private fun detail(owner: UserId = ownerId): InvoiceDetail {
        val invoice =
            Invoice(
                InvoiceId.reservation(paymentId), InvoiceType.RESERVATION, "Salle Étoile",
                BigDecimal("435.00"), Currency.EUR, Instant.parse("2026-07-10T10:00:00Z"), BookingId(UUID.randomUUID()),
            )
        return InvoiceDetail(invoice, owner, InvoiceBuyer("Jane Doe", "jane@example.com"))
    }

    @Test
    fun `returns NotFound when the id is unparseable`() {
        assertEquals(GetInvoiceDownloadResult.NotFound, sut.getDownloadUrl(InvoiceId("garbage"), ownerId))
        verify(exactly = 0) { invoiceRepository.findReservationDetail(any()) }
    }

    @Test
    fun `returns NotFound when the source is unknown or not settled`() {
        every { invoiceRepository.findReservationDetail(paymentId) } returns null

        assertEquals(
            GetInvoiceDownloadResult.NotFound,
            sut.getDownloadUrl(InvoiceId.reservation(paymentId), ownerId),
        )
    }

    @Test
    fun `returns NotOwner when the receipt belongs to another user`() {
        every { invoiceRepository.findReservationDetail(paymentId) } returns detail(owner = UserId(UUID.randomUUID()))

        assertEquals(
            GetInvoiceDownloadResult.NotOwner,
            sut.getDownloadUrl(InvoiceId.reservation(paymentId), ownerId),
        )
        verify(exactly = 0) { objectStorage.upload(ImageVisibility.PRIVATE, any(), any(), any()) }
    }

    @Test
    fun `generates and uploads the pdf lazily when the object is absent`() {
        every { invoiceRepository.findReservationDetail(paymentId) } returns detail()
        every { objectStorage.exists(ImageVisibility.PRIVATE, any()) } returns false
        every { pdfGenerator.generate(any(), any()) } returns byteArrayOf(1, 2, 3)
        every { objectStorage.signedUrl(any(), any()) } returns "https://signed/url"

        val result = assertInstanceOf<GetInvoiceDownloadResult.Found>(
            sut.getDownloadUrl(InvoiceId.reservation(paymentId), ownerId),
        )

        assertEquals("https://signed/url", result.downloadUrl)
        verify(exactly = 1) { pdfGenerator.generate(any(), any()) }
        verify(exactly = 1) { objectStorage.upload(ImageVisibility.PRIVATE, "invoices/PAY-${paymentId.value}.pdf", byteArrayOf(1, 2, 3), "application/pdf") }
    }

    @Test
    fun `does not regenerate the pdf when the object already exists`() {
        every { invoiceRepository.findReservationDetail(paymentId) } returns detail()
        every { objectStorage.exists(ImageVisibility.PRIVATE, any()) } returns true
        every { objectStorage.signedUrl(any(), any()) } returns "https://signed/existing"

        val result = assertInstanceOf<GetInvoiceDownloadResult.Found>(
            sut.getDownloadUrl(InvoiceId.reservation(paymentId), ownerId),
        )

        assertEquals("https://signed/existing", result.downloadUrl)
        verify(exactly = 0) { pdfGenerator.generate(any(), any()) }
        verify(exactly = 0) { objectStorage.upload(ImageVisibility.PRIVATE, any(), any(), any()) }
    }

    @Test
    fun `routes a cagnotte id to the share detail lookup`() {
        val shareId = PoolShareId(UUID.randomUUID())
        every { invoiceRepository.findCagnotteDetail(shareId) } returns null

        assertEquals(
            GetInvoiceDownloadResult.NotFound,
            sut.getDownloadUrl(InvoiceId.cagnotte(shareId), ownerId),
        )
        verify(exactly = 1) { invoiceRepository.findCagnotteDetail(shareId) }
    }

    @Test
    fun `signs the url with a short ttl`() {
        every { invoiceRepository.findReservationDetail(paymentId) } returns detail()
        every { objectStorage.exists(ImageVisibility.PRIVATE, any()) } returns true
        every { objectStorage.signedUrl(any(), any()) } returns "https://signed/url"

        sut.getDownloadUrl(InvoiceId.reservation(paymentId), ownerId)

        verify { objectStorage.signedUrl(any(), Duration.ofMinutes(15)) }
    }
}

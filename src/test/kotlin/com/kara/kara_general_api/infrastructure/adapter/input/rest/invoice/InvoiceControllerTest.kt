package com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadResult
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadUseCase
import com.kara.kara_general_api.domain.port.input.invoice.ListInvoicesUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val PAYMENT_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

@WebMvcTest(InvoiceController::class)
@Import(SecurityConfig::class)
class InvoiceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var listInvoicesUseCase: ListInvoicesUseCase

    @MockkBean
    private lateinit var getInvoiceDownloadUseCase: GetInvoiceDownloadUseCase

    private val invoiceId = InvoiceId.reservation(PaymentId(UUID.fromString(PAYMENT_ID)))

    private fun sampleInvoice() =
        Invoice(
            id = invoiceId,
            type = InvoiceType.RESERVATION,
            label = "Salle Étoile",
            amount = BigDecimal("435.00"),
            currency = Currency.EUR,
            issuedAt = Instant.parse("2026-07-10T10:00:00Z"),
            bookingId = BookingId(UUID.randomUUID()),
        )

    @Test
    fun `should return 401 when listing invoices without authentication`() {
        mockMvc.perform(get("/api/v1/invoices")).andExpect(status().isUnauthorized)

        verify(exactly = 0) { listInvoicesUseCase.listInvoices(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the list of invoices`() {
        every { listInvoicesUseCase.listInvoices(any()) } returns listOf(sampleInvoice())

        mockMvc.perform(get("/api/v1/invoices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].invoiceId").value(invoiceId.value))
            .andExpect(jsonPath("$[0].type").value("RESERVATION"))
            .andExpect(jsonPath("$[0].label").value("Salle Étoile"))
            .andExpect(jsonPath("$[0].amount").value(435.00))
            .andExpect(jsonPath("$[0].currency").value("EUR"))
            .andExpect(jsonPath("$[0].number").value(sampleInvoice().number()))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with an empty array when there is no invoice`() {
        every { listInvoicesUseCase.listInvoices(any()) } returns emptyList()

        mockMvc.perform(get("/api/v1/invoices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the signed download url`() {
        every { getInvoiceDownloadUseCase.getDownloadUrl(any(), any()) } returns
            GetInvoiceDownloadResult.Found("https://signed/url")

        mockMvc.perform(get("/api/v1/invoices/${invoiceId.value}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.downloadUrl").value("https://signed/url"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when the invoice belongs to another user`() {
        every { getInvoiceDownloadUseCase.getDownloadUrl(any(), any()) } returns GetInvoiceDownloadResult.NotOwner

        mockMvc.perform(get("/api/v1/invoices/${invoiceId.value}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("INVOICE_NOT_OWNER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when the invoice is unknown`() {
        every { getInvoiceDownloadUseCase.getDownloadUrl(any(), any()) } returns GetInvoiceDownloadResult.NotFound

        mockMvc.perform(get("/api/v1/invoices/PAY-unknown"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("INVOICE_NOT_FOUND"))
    }
}

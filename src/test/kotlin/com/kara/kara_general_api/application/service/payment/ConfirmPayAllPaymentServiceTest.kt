package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfirmPayAllPaymentServiceTest {
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val applyBookingExtensionService = mockk<ApplyBookingExtensionService>(relaxed = true)
    private val sut = ConfirmPayAllPaymentService(paymentRepository, bookingRepository, applyBookingExtensionService)

    private val bookingId = BookingId(UUID.randomUUID())

    private fun payment(
        status: PaymentStatus = PaymentStatus.PENDING,
        extensionId: BookingExtensionId? = null,
    ) = Payment(
        id = PaymentId(UUID.randomUUID()),
        bookingId = bookingId,
        userId = UserId(UUID.randomUUID()),
        amount = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        stripePaymentIntentId = "pi_1",
        createdAt = Instant.now(),
        extensionId = extensionId,
    )

    @Test
    fun `should mark the payment PAID and confirm the booking`() {
        assertTrue(sut.confirm(payment()))

        verify(exactly = 1) { paymentRepository.save(match { it.status == PaymentStatus.PAID }) }
        verify(exactly = 1) { bookingRepository.updateStatus(bookingId, BookingStatus.CONFIRMED) }
    }

    @Test
    fun `should reconfirm the booking without rewriting an already PAID payment`() {
        assertTrue(sut.confirm(payment(status = PaymentStatus.PAID)))

        verify(exactly = 0) { paymentRepository.save(any()) }
        verify(exactly = 1) { bookingRepository.updateStatus(bookingId, BookingStatus.CONFIRMED) }
    }

    @Test
    fun `should do nothing when the payment has been refunded`() {
        assertFalse(sut.confirm(payment(status = PaymentStatus.REFUNDED)))

        verify(exactly = 0) { paymentRepository.save(any()) }
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
        verify(exactly = 0) { applyBookingExtensionService.apply(any<BookingExtensionId>()) }
    }

    @Test
    fun `should apply the extension instead of confirming the booking when the payment carries one`() {
        val extensionId = BookingExtensionId(UUID.randomUUID())

        assertTrue(sut.confirm(payment(extensionId = extensionId)))

        verify(exactly = 1) { paymentRepository.save(match { it.status == PaymentStatus.PAID }) }
        verify(exactly = 1) { applyBookingExtensionService.apply(extensionId) }
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }
}

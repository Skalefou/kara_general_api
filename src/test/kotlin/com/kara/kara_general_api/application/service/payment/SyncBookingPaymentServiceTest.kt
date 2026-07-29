package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.payment.SyncBookingPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.SyncBookingPaymentResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentSnapshot
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SyncBookingPaymentServiceTest {
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>()
    private val applyBookingExtensionService = mockk<ApplyBookingExtensionService>(relaxed = true)

    // Confirmation branchée « en vrai » : le test doit observer la réservation réellement confirmée.
    private val confirmPayAllPaymentService =
        ConfirmPayAllPaymentService(paymentRepository, bookingRepository, applyBookingExtensionService)

    private val sut =
        SyncBookingPaymentService(
            paymentRepository,
            bookingRepository,
            paymentGateway,
            confirmPayAllPaymentService,
        )

    private val bookingId = BookingId(UUID.randomUUID())
    private val paymentId = PaymentId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    private fun payment(
        status: PaymentStatus = PaymentStatus.PENDING,
        owner: UserId = userId,
        booking: BookingId = bookingId,
    ) = Payment(
        id = paymentId,
        bookingId = booking,
        userId = owner,
        amount = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        stripePaymentIntentId = "pi_1",
        createdAt = Instant.now(),
    )

    private fun booking(status: BookingStatus = BookingStatus.PENDING) =
        Booking(
            id = bookingId,
            roomId = RoomId(UUID.randomUUID()),
            userId = userId,
            startAt = Instant.parse("2026-08-01T18:00:00Z"),
            endAt = Instant.parse("2026-08-01T21:30:00Z"),
            numberOfPeople = 8,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("435.00"),
            currency = Currency.EUR,
            status = status,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(900),
        )

    private fun snapshot(status: PaymentIntentStatus) =
        PaymentIntentSnapshot(paymentIntentId = "pi_1", status = status, clientSecret = "pi_1_secret")

    private fun command() = SyncBookingPaymentCommand(bookingId = bookingId, paymentId = paymentId, userId = userId)

    @Test
    fun `should confirm the booking when the payment intent succeeded`() {
        every { paymentRepository.findById(paymentId) } returnsMany
            listOf(payment(), payment(status = PaymentStatus.PAID))
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns snapshot(PaymentIntentStatus.SUCCEEDED)
        every { bookingRepository.findById(bookingId) } returns booking(status = BookingStatus.CONFIRMED)

        val result = sut.sync(command())

        val synced = assertIs<SyncBookingPaymentResult.Synced>(result)
        assertEquals(BookingStatus.CONFIRMED, synced.bookingStatus)
        assertEquals(PaymentStatus.PAID, synced.paymentStatus)
        verify(exactly = 1) { paymentRepository.save(match { it.status == PaymentStatus.PAID }) }
        verify(exactly = 1) { bookingRepository.updateStatus(bookingId, BookingStatus.CONFIRMED) }
    }

    @Test
    fun `should change nothing when the payment intent is not settled yet`() {
        every { paymentRepository.findById(paymentId) } returns payment()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns
            snapshot(PaymentIntentStatus.REQUIRES_PAYMENT_METHOD)
        every { bookingRepository.findById(bookingId) } returns booking()

        val result = sut.sync(command())

        val synced = assertIs<SyncBookingPaymentResult.Synced>(result)
        assertEquals(BookingStatus.PENDING, synced.bookingStatus)
        assertEquals(PaymentStatus.PENDING, synced.paymentStatus)
        verify(exactly = 0) { paymentRepository.save(any()) }
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `should change nothing when the payment intent cannot be read`() {
        every { paymentRepository.findById(paymentId) } returns payment()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns null
        every { bookingRepository.findById(bookingId) } returns booking()

        assertIs<SyncBookingPaymentResult.Synced>(sut.sync(command()))

        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `should return NotOwner when the payment belongs to another user`() {
        every { paymentRepository.findById(paymentId) } returns payment(owner = UserId(UUID.randomUUID()))

        assertEquals(SyncBookingPaymentResult.NotOwner, sut.sync(command()))

        verify(exactly = 0) { paymentGateway.retrievePaymentIntent(any()) }
    }

    @Test
    fun `should return NotFound when the payment does not exist`() {
        every { paymentRepository.findById(paymentId) } returns null

        assertEquals(SyncBookingPaymentResult.NotFound, sut.sync(command()))
    }

    @Test
    fun `should return NotFound when the payment belongs to another booking`() {
        every { paymentRepository.findById(paymentId) } returns payment(booking = BookingId(UUID.randomUUID()))

        assertEquals(SyncBookingPaymentResult.NotFound, sut.sync(command()))

        verify(exactly = 0) { paymentGateway.retrievePaymentIntent(any()) }
    }

    @Test
    fun `should return NotFound when the booking no longer exists`() {
        every { paymentRepository.findById(paymentId) } returns payment()
        every { paymentGateway.retrievePaymentIntent("pi_1") } returns snapshot(PaymentIntentStatus.REQUIRES_ACTION)
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(SyncBookingPaymentResult.NotFound, sut.sync(command()))
    }
}

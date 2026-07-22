package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.application.service.pool.PoolNotifier
import com.kara.kara_general_api.application.service.pool.PoolSettlementService
import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CancelBookingServiceTest {

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val poolSettlementService = mockk<PoolSettlementService>(relaxed = true)
    private val poolNotifier = mockk<PoolNotifier>(relaxed = true)
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val notificationService = mockk<NotificationService>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)

    private val sut =
        CancelBookingService(
            bookingRepository,
            poolRepository,
            poolShareRepository,
            paymentRepository,
            paymentGateway,
            poolSettlementService,
            poolNotifier,
            roomRepository,
            userRepository,
            notificationService,
            emailService,
        )

    private val ownerId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    private fun booking(
        status: BookingStatus,
        paymentMode: PaymentMode = PaymentMode.PAY_ALL,
        startAt: Instant = Instant.now().plusSeconds(3600),
    ) = Booking(
        id = bookingId,
        roomId = RoomId(UUID.randomUUID()),
        userId = ownerId,
        startAt = startAt,
        endAt = startAt.plusSeconds(7200),
        numberOfPeople = 8,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("100.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(900),
        paymentMode = paymentMode,
    )

    private fun command(requester: UserId = ownerId) =
        CancelBookingCommand(bookingId = bookingId, requesterId = requester)

    private fun openPool() =
        Pool(
            PoolId(UUID.randomUUID()), bookingId, BigDecimal("100.00"), Currency.EUR,
            PoolStatus.OPEN, Instant.now().plusSeconds(3600), "g", Instant.now(),
        )

    @Test
    fun `returns NotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(CancelBookingResult.NotFound, sut.cancel(command()))
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `returns NotOwner when the requester is not the owner`() {
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.PENDING)

        assertEquals(CancelBookingResult.NotOwner, sut.cancel(command(UserId(UUID.randomUUID()))))
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `returns AlreadyCancelled when the booking is already cancelled (idempotence)`() {
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.CANCELLED)

        assertEquals(CancelBookingResult.AlreadyCancelled, sut.cancel(command()))
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `returns AlreadyStarted when the reservation start is already past`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(BookingStatus.CONFIRMED, startAt = Instant.now().minusSeconds(60))

        assertEquals(CancelBookingResult.AlreadyStarted, sut.cancel(command()))
        verify(exactly = 0) { bookingRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `pay-all pending cancel releases nothing and is not refunded`() {
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.PENDING)
        every { poolRepository.findByBookingId(bookingId) } returns null

        val result = assertInstanceOf<CancelBookingResult.Cancelled>(sut.cancel(command()))

        assertFalse(result.refunded)
        assertEquals(BookingStatus.CANCELLED, result.booking.status)
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CANCELLED) }
        verify(exactly = 0) { paymentGateway.refundPaymentIntent(any()) }
        verify(exactly = 0) { poolSettlementService.cancelShareHolds(any()) }
        verify(exactly = 0) { poolSettlementService.refundCapturedShares(any()) }
    }

    @Test
    fun `open shared-pot cancel releases all holds without charge and is not refunded`() {
        val pool = openPool()
        val shares =
            listOf(
                PoolShare(PoolShareId(UUID.randomUUID()), pool.id, "A", null, BigDecimal("50.00"), PoolShareStatus.AUTHORIZED, "pi_a", null, null, false),
                PoolShare(PoolShareId(UUID.randomUUID()), pool.id, "B", null, BigDecimal("50.00"), PoolShareStatus.PENDING, null, null, null, false),
            )
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.PENDING, PaymentMode.SHARED_POT)
        every { poolRepository.findByBookingId(bookingId) } returns pool
        every { poolShareRepository.findByPoolId(pool.id) } returns shares

        val result = assertInstanceOf<CancelBookingResult.Cancelled>(sut.cancel(command()))

        assertFalse(result.refunded)
        verify(exactly = 1) { poolSettlementService.cancelShareHolds(shares) }
        verify { poolRepository.updateStatus(pool.id, PoolStatus.CANCELLED) }
        verify { poolNotifier.notifyPoolCancelled(any(), shares) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CANCELLED) }
        verify(exactly = 0) { paymentGateway.refundPaymentIntent(any()) }
    }

    @Test
    fun `confirmed pay-all cancel fully refunds the captured payment`() {
        val payment =
            Payment(
                PaymentId(UUID.randomUUID()), bookingId, ownerId, BigDecimal("100.00"),
                Currency.EUR, PaymentStatus.PAID, "pi_pay", Instant.now(),
            )
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.CONFIRMED)
        every { poolRepository.findByBookingId(bookingId) } returns null
        every { paymentRepository.findByBookingId(bookingId) } returns listOf(payment)

        val result = assertInstanceOf<CancelBookingResult.Cancelled>(sut.cancel(command()))

        assertTrue(result.refunded)
        verify(exactly = 1) { paymentGateway.refundPaymentIntent("pi_pay") }
        verify { paymentRepository.save(match { it.status == PaymentStatus.REFUNDED }) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CANCELLED) }
    }

    @Test
    fun `confirmed shared-pot cancel refunds every captured share`() {
        val pool = openPool()
        val shares =
            listOf(
                PoolShare(PoolShareId(UUID.randomUUID()), pool.id, "A", null, BigDecimal("50.00"), PoolShareStatus.CAPTURED, "pi_a", null, null, false),
                PoolShare(PoolShareId(UUID.randomUUID()), pool.id, "B", null, BigDecimal("50.00"), PoolShareStatus.CAPTURED, "pi_b", null, null, false),
            )
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.CONFIRMED, PaymentMode.SHARED_POT)
        every { poolRepository.findByBookingId(bookingId) } returns pool
        every { poolShareRepository.findByPoolId(pool.id) } returns shares

        val result = assertInstanceOf<CancelBookingResult.Cancelled>(sut.cancel(command()))

        assertTrue(result.refunded)
        verify(exactly = 1) { poolSettlementService.refundCapturedShares(shares) }
        verify { poolRepository.updateStatus(pool.id, PoolStatus.CANCELLED) }
        verify { poolNotifier.notifyPoolCancelled(any(), shares) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CANCELLED) }
        verify(exactly = 0) { paymentGateway.refundPaymentIntent(any()) }
    }
}

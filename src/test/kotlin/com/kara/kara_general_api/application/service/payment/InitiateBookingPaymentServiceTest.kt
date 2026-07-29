package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.kara.kara_general_api.domain.port.output.PaymentIntentSnapshot
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InitiateBookingPaymentServiceTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val userRepository = mockk<UserRepository>()
    private val paymentGateway = mockk<PaymentGateway>()
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val sut = InitiateBookingPaymentService(bookingRepository, userRepository, paymentGateway, paymentRepository)

    private val bookingId = BookingId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    private fun booking(
        status: BookingStatus = BookingStatus.PENDING,
        owner: UserId = userId,
        expiresAt: Instant = Instant.now().plusSeconds(900),
    ) = Booking(
        id = bookingId,
        roomId = RoomId(UUID.randomUUID()),
        userId = owner,
        startAt = Instant.parse("2026-08-01T18:00:00Z"),
        endAt = Instant.parse("2026-08-01T21:30:00Z"),
        numberOfPeople = 8,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now(),
        expiresAt = expiresAt,
    )

    private fun user(stripeCustomerId: String? = null) =
        User(
            id = userId,
            email = Email("jane@example.com"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("+33612345678"),
            birthDate = LocalDate.of(1990, 1, 1),
            role = UserRole.CLIENT,
            firebaseUid = "uid",
            createdAt = Instant.now(),
            stripeCustomerId = stripeCustomerId,
        )

    private fun command() = InitiateBookingPaymentCommand(bookingId = bookingId, userId = userId)

    @Test
    fun `should return BookingNotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(InitiateBookingPaymentResult.BookingNotFound, sut.initiate(command()))
    }

    @Test
    fun `should return NotOwner when the booking belongs to another user`() {
        every { bookingRepository.findById(bookingId) } returns booking(owner = UserId(UUID.randomUUID()))

        assertEquals(InitiateBookingPaymentResult.NotOwner, sut.initiate(command()))
    }

    @Test
    fun `should return AlreadyPaid when the booking is not PENDING`() {
        every { bookingRepository.findById(bookingId) } returns booking(status = BookingStatus.CONFIRMED)

        assertEquals(InitiateBookingPaymentResult.AlreadyPaid, sut.initiate(command()))
    }

    @Test
    fun `should return BookingExpired when the payment window is over`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(status = BookingStatus.PENDING, expiresAt = Instant.now().minusSeconds(60))

        assertEquals(InitiateBookingPaymentResult.BookingExpired, sut.initiate(command()))
    }

    @Test
    fun `should create the customer lazily and persist a pending payment`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { userRepository.findById(userId) } returns user(stripeCustomerId = null)
        every { paymentGateway.ensureCustomer(any()) } returns "cus_123"
        every { userRepository.updateStripeCustomerId(userId, "cus_123") } returns Unit
        every { paymentGateway.createEphemeralKey("cus_123") } returns "ek_secret"
        every { paymentRepository.findPendingByBookingId(bookingId) } returns null
        every {
            paymentGateway.createPaymentIntent(BigDecimal("435.00"), Currency.EUR, "cus_123", any())
        } returns PaymentIntentResult(clientSecret = "pi_secret", paymentIntentId = "pi_1")
        every { paymentGateway.publishableKey() } returns "pk_test"
        every { paymentRepository.save(any()) } answers { firstArg<Payment>() }

        val result = sut.initiate(command())

        val ready = assertIs<InitiateBookingPaymentResult.Ready>(result)
        assertEquals("pi_secret", ready.clientSecret)
        assertEquals("ek_secret", ready.ephemeralKeySecret)
        assertEquals("cus_123", ready.customerId)
        assertEquals("pk_test", ready.publishableKey)
        verify(exactly = 1) { userRepository.updateStripeCustomerId(userId, "cus_123") }
        verify(exactly = 1) { paymentRepository.save(any()) }
    }

    @Test
    fun `should send a deterministic idempotency key when creating the payment intent`() {
        stubNewIntentFlow()
        val keySlot = slot<String>()
        every {
            paymentGateway.createPaymentIntent(any(), any(), any(), capture(keySlot))
        } returns PaymentIntentResult(clientSecret = "pi_secret", paymentIntentId = "pi_1")

        sut.initiate(command())

        assertEquals("booking-payment-${bookingId.value}", keySlot.captured)
    }

    @Test
    fun `should reuse the existing stripe customer without persisting again`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { userRepository.findById(userId) } returns user(stripeCustomerId = "cus_existing")
        every { paymentGateway.ensureCustomer(any()) } returns "cus_existing"
        every { paymentGateway.createEphemeralKey("cus_existing") } returns "ek_secret"
        every { paymentRepository.findPendingByBookingId(bookingId) } returns null
        every { paymentGateway.createPaymentIntent(any(), any(), any(), any()) } returns
            PaymentIntentResult(clientSecret = "pi_secret", paymentIntentId = "pi_1")
        every { paymentGateway.publishableKey() } returns "pk_test"
        every { paymentRepository.save(any()) } answers { firstArg<Payment>() }

        sut.initiate(command())

        verify(exactly = 0) { userRepository.updateStripeCustomerId(any(), any()) }
    }

    @Test
    fun `should reuse the pending payment intent instead of creating a second one`() {
        stubNewIntentFlow()
        val existing = pendingPayment()
        every { paymentRepository.findPendingByBookingId(bookingId) } returns existing
        every { paymentGateway.retrievePaymentIntent("pi_existing") } returns
            PaymentIntentSnapshot(
                paymentIntentId = "pi_existing",
                status = PaymentIntentStatus.REQUIRES_PAYMENT_METHOD,
                clientSecret = "pi_existing_secret",
            )

        val ready = assertIs<InitiateBookingPaymentResult.Ready>(sut.initiate(command()))

        assertEquals("pi_existing_secret", ready.clientSecret)
        assertEquals(existing.id.value, ready.paymentId)
        verify(exactly = 0) { paymentGateway.createPaymentIntent(any(), any(), any(), any()) }
        verify(exactly = 0) { paymentRepository.save(any()) }
    }

    @Test
    fun `should create a new payment intent when the pending one is no longer payable`() {
        stubNewIntentFlow()
        every { paymentRepository.findPendingByBookingId(bookingId) } returns pendingPayment()
        every { paymentGateway.retrievePaymentIntent("pi_existing") } returns
            PaymentIntentSnapshot(
                paymentIntentId = "pi_existing",
                status = PaymentIntentStatus.CANCELED,
                clientSecret = "pi_existing_secret",
            )

        val ready = assertIs<InitiateBookingPaymentResult.Ready>(sut.initiate(command()))

        assertEquals("pi_secret", ready.clientSecret)
        verify(exactly = 1) { paymentGateway.createPaymentIntent(any(), any(), any(), any()) }
        verify(exactly = 1) { paymentRepository.save(any()) }
    }

    private fun pendingPayment() =
        Payment.pending(
            bookingId = bookingId,
            userId = userId,
            amount = BigDecimal("435.00"),
            currency = Currency.EUR,
            stripePaymentIntentId = "pi_existing",
        )

    /** Chemin nominal : client Stripe connu, aucun intent réutilisable, création d'un nouvel intent. */
    private fun stubNewIntentFlow() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { userRepository.findById(userId) } returns user(stripeCustomerId = "cus_existing")
        every { paymentGateway.ensureCustomer(any()) } returns "cus_existing"
        every { paymentGateway.createEphemeralKey("cus_existing") } returns "ek_secret"
        every { paymentRepository.findPendingByBookingId(bookingId) } returns null
        every { paymentGateway.createPaymentIntent(any(), any(), any(), any()) } returns
            PaymentIntentResult(clientSecret = "pi_secret", paymentIntentId = "pi_1")
        every { paymentGateway.publishableKey() } returns "pk_test"
        every { paymentRepository.save(any()) } answers { firstArg<Payment>() }
    }
}

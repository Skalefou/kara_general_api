package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolCommand
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolShareInput
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.LinkTokenGenerator
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreatePoolServiceTest {

    private val bookingRepository = mockk<BookingRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val linkTokenGenerator = mockk<LinkTokenGenerator>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        CreatePoolService(
            bookingRepository,
            roomRepository,
            poolRepository,
            poolShareRepository,
            linkTokenGenerator,
            emailService,
        )

    private val bookingId = BookingId(UUID.randomUUID())
    private val creatorId = UserId(UUID.randomUUID())

    private fun booking(
        status: BookingStatus = BookingStatus.PENDING,
        mode: PaymentMode = PaymentMode.SHARED_POT,
        owner: UserId = creatorId,
        startAt: Instant = Instant.now().plusSeconds(6 * 3600),
        total: BigDecimal = BigDecimal("100.00"),
    ) = Booking(
        id = bookingId,
        roomId = RoomId(UUID.randomUUID()),
        userId = owner,
        startAt = startAt,
        endAt = startAt.plusSeconds(3600),
        numberOfPeople = 4,
        selectedOptionIds = emptyList(),
        totalPrice = total,
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(24 * 3600),
        paymentMode = mode,
    )

    private fun command(vararg shares: CreatePoolShareInput) =
        CreatePoolCommand(bookingId = bookingId, creatorId = creatorId, shares = shares.toList())

    private fun stubHappyDependencies() {
        every { poolRepository.findByBookingId(bookingId) } returns null
        every { linkTokenGenerator.generate() } returnsMany listOf("global", "tok-a", "tok-b")
        every { poolRepository.save(any()) } answers { firstArg<Pool>() }
        every { poolShareRepository.saveAll(any()) } answers { firstArg<List<PoolShare>>() }
        val room = mockk<Room>()
        every { room.name } returns "Salle Étoile"
        every { roomRepository.findById(any()) } returns room
    }

    @Test
    fun `returns BookingNotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(CreatePoolResult.BookingNotFound, sut.create(command()))
    }

    @Test
    fun `returns NotOwner when the booking belongs to another client`() {
        every { bookingRepository.findById(bookingId) } returns booking(owner = UserId(UUID.randomUUID()))

        assertEquals(CreatePoolResult.NotOwner, sut.create(command()))
    }

    @Test
    fun `returns BookingNotPending when the booking is not pending`() {
        every { bookingRepository.findById(bookingId) } returns booking(status = BookingStatus.CONFIRMED)

        assertEquals(CreatePoolResult.BookingNotPending, sut.create(command()))
    }

    @Test
    fun `returns NotSharedPot when the booking was not created in shared-pot mode`() {
        every { bookingRepository.findById(bookingId) } returns booking(mode = PaymentMode.PAY_ALL)

        assertEquals(CreatePoolResult.NotSharedPot, sut.create(command()))
    }

    @Test
    fun `returns PoolAlreadyExists when a pool is already attached to the booking`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { poolRepository.findByBookingId(bookingId) } returns mockk()

        assertEquals(CreatePoolResult.PoolAlreadyExists, sut.create(command(
            CreatePoolShareInput("Alice", null, BigDecimal("100.00"), isCreatorShare = true),
        )))
    }

    @Test
    fun `returns SharesMismatch when the shares do not sum to the target`() {
        every { bookingRepository.findById(bookingId) } returns booking(total = BigDecimal("100.00"))
        every { poolRepository.findByBookingId(bookingId) } returns null

        val result = sut.create(command(
            CreatePoolShareInput("Alice", null, BigDecimal("40.00"), isCreatorShare = true),
            CreatePoolShareInput("Bob", "bob@example.com", BigDecimal("50.00")),
        ))

        val mismatch = assertIs<CreatePoolResult.SharesMismatch>(result)
        assertEquals(BigDecimal("100.00"), mismatch.expected)
        assertEquals(BigDecimal("90.00"), mismatch.actual)
    }

    @Test
    fun `returns ReservationTooClose when the computed deadline is already past`() {
        every { bookingRepository.findById(bookingId) } returns booking(startAt = Instant.now().plusSeconds(600))
        every { poolRepository.findByBookingId(bookingId) } returns null

        val result = sut.create(command(
            CreatePoolShareInput("Alice", null, BigDecimal("100.00"), isCreatorShare = true),
        ))

        assertEquals(CreatePoolResult.ReservationTooClose, result)
    }

    @Test
    fun `creates the pool, persists shares and emails non-creator participants with an email`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        stubHappyDependencies()
        val savedShares = slot<List<PoolShare>>()
        every { poolShareRepository.saveAll(capture(savedShares)) } answers { firstArg() }

        val result = sut.create(command(
            CreatePoolShareInput("Alice (moi)", null, BigDecimal("60.00"), isCreatorShare = true),
            CreatePoolShareInput("Bob", "bob@example.com", BigDecimal("40.00")),
        ))

        val created = assertIs<CreatePoolResult.Created>(result)
        assertEquals(BigDecimal("100.00"), created.view.targetAmount)
        assertEquals(2, savedShares.captured.size)
        verify(exactly = 1) { poolRepository.save(any()) }
        // Only the non-creator participant with an email receives an invitation.
        verify(exactly = 1) {
            emailService.sendPoolInvitation(Email("bob@example.com"), "Bob", "Salle Étoile", any(), any())
        }
    }
}

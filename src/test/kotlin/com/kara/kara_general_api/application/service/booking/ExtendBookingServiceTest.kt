package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingResult
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExtendBookingServiceTest {

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val bookingExtensionRepository = mockk<BookingExtensionRepository>(relaxed = true)
    private val extensionFeasibility = mockk<ExtensionFeasibility>()
    private val sut =
        ExtendBookingService(
            bookingRepository,
            roomRepository,
            bookingExtensionRepository,
            extensionFeasibility,
        )

    private val ownerId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())
    private val roomId = RoomId.generate()

    private val room =
        Room(
            id = roomId,
            name = "Salle",
            description = "",
            address = Address("1 rue", "Paris", "75001", "France"),
            pricePerPersonPerHour = BigDecimal("10.00"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = false,
            createdAt = Instant.now(),
            timeZone = ZoneId.of("UTC"),
        )

    private fun booking(
        status: BookingStatus = BookingStatus.CONFIRMED,
        endAt: Instant = Instant.now().plus(Duration.ofHours(1)),
    ) = Booking(
        id = bookingId,
        roomId = roomId,
        userId = ownerId,
        startAt = Instant.now().minus(Duration.ofHours(1)),
        endAt = endAt,
        numberOfPeople = 4,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("80.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now(),
        expiresAt = Instant.now(),
    )

    private fun command(
        additionalMinutes: Int = 60,
        paymentMode: PaymentMode = PaymentMode.PAY_ALL,
        userId: UserId = ownerId,
    ) = ExtendBookingCommand(
        bookingId = bookingId,
        currentUserId = userId,
        additionalMinutes = additionalMinutes,
        paymentMode = paymentMode,
    )

    @Test
    fun `should create a pending extension priced on the additional hours`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { bookingExtensionRepository.findPendingByBookingId(bookingId) } returns null
        every { roomRepository.findById(roomId) } returns room
        every { extensionFeasibility.maxAdditionalMinutes(any(), any(), any()) } returns 120
        val saved = slot<BookingExtension>()
        every { bookingExtensionRepository.save(capture(saved)) } answers { saved.captured }

        val result = sut.extend(command(additionalMinutes = 60))

        val created = assertIs<ExtendBookingResult.Created>(result)
        assertEquals(BigDecimal("40.00"), created.extension.price)
        assertEquals(60, created.extension.additionalMinutes)
        verify { bookingExtensionRepository.save(any()) }
    }

    @Test
    fun `should refuse a duration longer than the available slot`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { bookingExtensionRepository.findPendingByBookingId(bookingId) } returns null
        every { roomRepository.findById(roomId) } returns room
        every { extensionFeasibility.maxAdditionalMinutes(any(), any(), any()) } returns 30

        val result = sut.extend(command(additionalMinutes = 120))

        val unavailable = assertIs<ExtendBookingResult.SlotUnavailable>(result)
        assertEquals(30, unavailable.maxAdditionalMinutes)
    }

    @Test
    fun `should refuse when another extension is already awaiting payment`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { bookingExtensionRepository.findPendingByBookingId(bookingId) } returns
            BookingExtension.create(
                bookingId = bookingId,
                userId = ownerId,
                additionalMinutes = 30,
                previousEndAt = Instant.now().plus(Duration.ofHours(1)),
                price = BigDecimal("20.00"),
                currency = Currency.EUR,
                paymentMode = PaymentMode.PAY_ALL,
                now = Instant.now(),
            )

        val result = sut.extend(command())

        assertEquals(ExtendBookingResult.ExtensionAlreadyPending, result)
    }

    @Test
    fun `should refuse when a concurrent request already inserted a pending extension`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { bookingExtensionRepository.findPendingByBookingId(bookingId) } returns null
        every { roomRepository.findById(roomId) } returns room
        every { extensionFeasibility.maxAdditionalMinutes(any(), any(), any()) } returns 120
        every { bookingExtensionRepository.save(any()) } throws
            DataIntegrityViolationException("idx_booking_extensions_pending_booking")

        val result = sut.extend(command(additionalMinutes = 60))

        assertEquals(ExtendBookingResult.ExtensionAlreadyPending, result)
    }

    @Test
    fun `should refuse when the caller does not own the booking`() {
        every { bookingRepository.findById(bookingId) } returns booking()

        val result = sut.extend(command(userId = UserId(UUID.randomUUID())))

        assertEquals(ExtendBookingResult.NotOwner, result)
    }

    @Test
    fun `should refuse when the booking is not confirmed`() {
        every { bookingRepository.findById(bookingId) } returns booking(status = BookingStatus.PENDING)

        val result = sut.extend(command())

        assertEquals(ExtendBookingResult.BookingNotConfirmed, result)
    }

    @Test
    fun `should refuse when the booking is already over`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(endAt = Instant.now().minus(Duration.ofMinutes(1)))

        val result = sut.extend(command())

        assertEquals(ExtendBookingResult.BookingNotActive, result)
    }

    @Test
    fun `should refuse when no time is left to settle before the booking ends`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(endAt = Instant.now().plus(Duration.ofMinutes(2)))
        every { bookingExtensionRepository.findPendingByBookingId(bookingId) } returns null
        every { roomRepository.findById(roomId) } returns room
        every { extensionFeasibility.maxAdditionalMinutes(any(), any(), any()) } returns 120

        val result = sut.extend(command())

        assertEquals(ExtendBookingResult.SettlementWindowTooShort, result)
    }
}

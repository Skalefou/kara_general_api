package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
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

class CreateBookingServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val roomOptionRepository = mockk<RoomOptionRepository>()
    private val bookingRepository = mockk<BookingRepository>()
    private val sut = CreateBookingService(roomRepository, roomOptionRepository, bookingRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")
    private val end = start.plusSeconds(2 * 3600)

    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            description = "Grande salle",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            pricePerPersonPerHour = BigDecimal("12.50"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
            createdAt = Instant.now(),
        )

    private fun command(
        numberOfPeople: Int = 4,
        optionIds: List<RoomOptionId> = emptyList(),
    ) = CreateBookingCommand(
        roomId = roomId,
        userId = userId,
        startAt = start,
        endAt = end,
        numberOfPeople = numberOfPeople,
        selectedOptionIds = optionIds,
    )

    @Test
    fun `should return RoomNotFound when the room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.createBooking(command())

        assertEquals(CreateBookingResult.RoomNotFound, result)
        verify(exactly = 0) { bookingRepository.save(any()) }
    }

    @Test
    fun `should return TooFewPeople when fewer than two people`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns emptyList()

        val result = sut.createBooking(command(numberOfPeople = 1))

        assertEquals(CreateBookingResult.TooFewPeople, result)
        verify(exactly = 0) { bookingRepository.save(any()) }
    }

    @Test
    fun `should return CapacityExceeded when above the room capacity`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns emptyList()

        val result = sut.createBooking(command(numberOfPeople = 999))

        val exceeded = assertIs<CreateBookingResult.CapacityExceeded>(result)
        assertEquals(50, exceeded.maxCapacity)
    }

    @Test
    fun `should return DurationTooShort when the slot is shorter than one hour`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns emptyList()

        val result = sut.createBooking(command().copy(endAt = start.plusSeconds(30 * 60)))

        val tooShort = assertIs<CreateBookingResult.DurationTooShort>(result)
        assertEquals(60L, tooShort.minimumMinutes)
        verify(exactly = 0) { bookingRepository.save(any()) }
    }

    @Test
    fun `should return UnknownOptions when an option does not belong to the room`() {
        val stranger = RoomOptionId(UUID.randomUUID())
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns emptyList()

        val result = sut.createBooking(command(optionIds = listOf(stranger)))

        val unknown = assertIs<CreateBookingResult.UnknownOptions>(result)
        assertEquals(listOf(stranger.value), unknown.optionIds)
    }

    @Test
    fun `should return SlotUnavailable when the slot overlaps an existing booking`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns emptyList()
        every { bookingRepository.existsOverlapping(roomId, start, end) } returns true

        val result = sut.createBooking(command())

        assertEquals(CreateBookingResult.SlotUnavailable, result)
        verify(exactly = 0) { bookingRepository.save(any()) }
    }

    @Test
    fun `should persist a PENDING booking with the estimated total price`() {
        val optionId = RoomOptionId(UUID.randomUUID())
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns
            listOf(
                RoomOption(
                    id = optionId,
                    roomId = roomId,
                    label = "Ménage",
                    description = null,
                    price = BigDecimal("60.00"),
                    currency = Currency.EUR,
                ),
            )
        every { bookingRepository.existsOverlapping(roomId, start, end) } returns false
        val saved = slot<com.kara.kara_general_api.domain.model.booking.Booking>()
        every { bookingRepository.save(capture(saved)) } answers { saved.captured }

        // 12.50 * 4 * 2h = 100.00 ; option 60.00 ; total 160.00
        val result = sut.createBooking(command(numberOfPeople = 4, optionIds = listOf(optionId)))

        val created = assertIs<CreateBookingResult.Created>(result)
        assertEquals(BookingStatus.PENDING, created.booking.status)
        assertEquals(BigDecimal("160.00"), created.booking.totalPrice)
        assertEquals(Currency.EUR, created.booking.currency)
        assertEquals(listOf(optionId), created.booking.selectedOptionIds)
        assertEquals(userId, created.booking.userId)
    }
}

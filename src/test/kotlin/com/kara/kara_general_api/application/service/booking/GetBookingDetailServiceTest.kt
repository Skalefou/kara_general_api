package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.booking.ticketCode
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomStatus
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GetBookingDetailServiceTest {
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val sut = GetBookingDetailService(bookingRepository, roomRepository)

    private val ownerId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())

    private fun booking(status: BookingStatus) =
        Booking(
            id = bookingId,
            roomId = roomId,
            userId = ownerId,
            startAt = Instant.parse("2026-08-01T18:00:00Z"),
            endAt = Instant.parse("2026-08-01T21:00:00Z"),
            numberOfPeople = 8,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("100.00"),
            currency = Currency.EUR,
            status = status,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(900),
            paymentMode = PaymentMode.PAY_ALL,
        )

    private fun room() =
        Room(
            id = roomId,
            name = "Salle Étoile",
            description = "desc",
            address = Address("12 rue de Paris", "Lyon", "69002", "France"),
            pricePerPersonPerHour = BigDecimal("10.00"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = false,
            createdAt = Instant.now(),
            status = RoomStatus.OPEN,
        )

    @Test
    fun `returns NotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(GetBookingDetailResult.NotFound, sut.getDetail(bookingId, ownerId))
    }

    @Test
    fun `returns NotOwner when the requester is not the owner`() {
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.CONFIRMED)

        assertEquals(GetBookingDetailResult.NotOwner, sut.getDetail(bookingId, UserId(UUID.randomUUID())))
    }

    @Test
    fun `exposes the ticket code and formatted address when the booking is confirmed`() {
        val confirmed = booking(BookingStatus.CONFIRMED)
        every { bookingRepository.findById(bookingId) } returns confirmed
        every { roomRepository.findById(roomId) } returns room()

        val result = assertInstanceOf<GetBookingDetailResult.Found>(sut.getDetail(bookingId, ownerId))

        assertNotNull(result.view.ticketCode)
        assertEquals(confirmed.ticketCode(), result.view.ticketCode)
        assertEquals("Salle Étoile", result.view.roomName)
        assertEquals("12 rue de Paris, 69002 Lyon, France", result.view.roomAddress)
    }

    @Test
    fun `hides the ticket code when the booking is not confirmed`() {
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.PENDING)
        every { roomRepository.findById(roomId) } returns room()

        val result = assertInstanceOf<GetBookingDetailResult.Found>(sut.getDetail(bookingId, ownerId))

        assertNull(result.view.ticketCode)
    }

    @Test
    fun `falls back gracefully when the room is missing`() {
        every { bookingRepository.findById(bookingId) } returns booking(BookingStatus.PENDING)
        every { roomRepository.findById(roomId) } returns null

        val result = assertInstanceOf<GetBookingDetailResult.Found>(sut.getDetail(bookingId, ownerId))

        assertEquals("Salle", result.view.roomName)
        assertNull(result.view.roomAddress)
    }
}

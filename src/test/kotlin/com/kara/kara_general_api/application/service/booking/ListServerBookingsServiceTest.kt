package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ListServerBookingsServiceTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListServerBookingsService(bookingRepository, roomRepository)

    private val serverId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())

    private fun booking() =
        Booking(
            id = BookingId(UUID.randomUUID()),
            roomId = roomId,
            userId = UserId(UUID.randomUUID()),
            startAt = Instant.parse("2026-08-01T18:00:00Z"),
            endAt = Instant.parse("2026-08-01T21:00:00Z"),
            numberOfPeople = 8,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("240.00"),
            currency = Currency.EUR,
            status = BookingStatus.CONFIRMED,
            createdAt = Instant.now(),
            expiresAt = Instant.now(),
        )

    @Test
    fun `should enrich assigned bookings with the room name`() {
        val booking = booking()
        every { bookingRepository.findAssignedToServer(serverId) } returns listOf(booking)
        every { roomRepository.findById(roomId) } returns
            Room(
                id = roomId,
                name = "Salle Lune",
                description = "desc",
                address = Address(street = "2 rue", city = "Lyon", postalCode = "69001", country = "France"),
                pricePerPersonPerHour = BigDecimal("10.00"),
                currency = Currency.EUR,
                maxCapacity = 30,
                isThereWifi = true,
                isThereSonoPro = false,
                isThereAirConditioning = false,
                createdAt = Instant.now(),
            )

        val result = sut.listServerBookings(serverId)

        assertEquals(1, result.size)
        assertEquals("Salle Lune", result.first().roomName)
        assertEquals(booking, result.first().booking)
    }

    @Test
    fun `should return empty when the server has no assigned bookings`() {
        every { bookingRepository.findAssignedToServer(serverId) } returns emptyList()

        assertEquals(emptyList(), sut.listServerBookings(serverId))
    }
}

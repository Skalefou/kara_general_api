package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class ListAllBookingsServiceTest {

    private val bookingRepository = mockk<BookingRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val userRepository = mockk<UserRepository>()
    private val sut = ListAllBookingsService(bookingRepository, roomRepository, userRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val clientId = UserId(UUID.randomUUID())

    @Test
    fun `should enrich every booking with room and client names`() {
        val booking =
            Booking(
                id = BookingId(UUID.randomUUID()),
                roomId = roomId,
                userId = clientId,
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
        every { bookingRepository.findAllBookings() } returns listOf(booking)
        every { roomRepository.findById(roomId) } returns
            Room(
                id = roomId,
                name = "Salle Étoile",
                description = "desc",
                address = Address(street = "1 rue", city = "Paris", postalCode = "75001", country = "France"),
                pricePerPersonPerHour = BigDecimal("10.00"),
                currency = Currency.EUR,
                maxCapacity = 20,
                isThereWifi = true,
                isThereSonoPro = false,
                isThereAirConditioning = false,
                createdAt = Instant.now(),
            )
        every { userRepository.findById(clientId) } returns
            User(
                id = clientId,
                email = Email("client@kara.app"),
                hashedPassword = HashedPassword("hashed"),
                firstName = "Marie",
                lastName = "Client",
                phoneNumber = PhoneNumber("0612345678"),
                birthDate = LocalDate.of(1992, 2, 2),
                role = UserRole.CLIENT,
                firebaseUid = "uid",
                createdAt = Instant.now(),
            )

        val result = sut.listAllBookings()

        assertEquals(1, result.size)
        assertEquals("Salle Étoile", result.first().roomName)
        assertEquals("Marie Client", result.first().clientName)
        assertEquals(booking, result.first().booking)
    }

    @Test
    fun `should fall back to placeholders when room or client is missing`() {
        val booking =
            Booking(
                id = BookingId(UUID.randomUUID()),
                roomId = roomId,
                userId = clientId,
                startAt = Instant.now(),
                endAt = Instant.now().plusSeconds(3600),
                numberOfPeople = 2,
                selectedOptionIds = emptyList(),
                totalPrice = BigDecimal("20.00"),
                currency = Currency.EUR,
                status = BookingStatus.PENDING,
                createdAt = Instant.now(),
                expiresAt = Instant.now(),
            )
        every { bookingRepository.findAllBookings() } returns listOf(booking)
        every { roomRepository.findById(roomId) } returns null
        every { userRepository.findById(clientId) } returns null

        val result = sut.listAllBookings()

        assertEquals("Salle inconnue", result.first().roomName)
        assertEquals("Client inconnu", result.first().clientName)
    }
}

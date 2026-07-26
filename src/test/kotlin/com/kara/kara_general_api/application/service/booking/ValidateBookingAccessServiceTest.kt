package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckIn
import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckInId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.ticketCode
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
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessCommand
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessResult
import com.kara.kara_general_api.domain.port.output.BookingAccessCheckInRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidateBookingAccessServiceTest {
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val serverShiftRepository = mockk<ServerShiftRepository>(relaxed = true)
    private val checkInRepository = mockk<BookingAccessCheckInRepository>(relaxed = true)

    private val sut =
        ValidateBookingAccessService(
            bookingRepository,
            roomRepository,
            userRepository,
            serverShiftRepository,
            checkInRepository,
        )

    private val bookingId = BookingId(UUID.randomUUID())
    private val roomId = RoomId.generate()
    private val clientId = UserId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())

    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
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

    private fun user(
        id: UserId,
        first: String,
        last: String,
        role: UserRole,
    ) = User(
        id = id,
        email = Email("${first.lowercase()}@example.com"),
        hashedPassword = HashedPassword("hashed"),
        firstName = first,
        lastName = last,
        phoneNumber = PhoneNumber("+33612345678"),
        birthDate = LocalDate.of(1990, 1, 15),
        role = role,
        firebaseUid = "uid-$first",
        createdAt = Instant.now(),
        emailVerified = true,
    )

    private fun booking(
        status: BookingStatus = BookingStatus.CONFIRMED,
        startAt: Instant = Instant.now().minus(Duration.ofMinutes(10)),
        endAt: Instant = Instant.now().plus(Duration.ofHours(2)),
    ) = Booking(
        id = bookingId,
        roomId = roomId,
        userId = clientId,
        startAt = startAt,
        endAt = endAt,
        numberOfPeople = 6,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("120.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now().minus(Duration.ofDays(1)),
        expiresAt = Instant.now().minus(Duration.ofDays(1)),
    )

    private fun command(
        userId: UserId = serverId,
        isAdmin: Boolean = false,
    ) = ValidateBookingAccessCommand(bookingId = bookingId, currentUserId = userId, isAdmin = isAdmin)

    private fun stubAssignedAndLookups() {
        every { checkInRepository.findByBookingId(bookingId) } returns null
        every { roomRepository.findById(roomId) } returns room
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns setOf(serverId)
        every { userRepository.findById(clientId) } returns user(clientId, "Alice", "Martin", UserRole.CLIENT)
        every { userRepository.findById(serverId) } returns user(serverId, "Bob", "Durand", UserRole.SERVER)
    }

    @Test
    fun `grants access and records the check-in for an assigned server`() {
        val current = booking()
        every { bookingRepository.findById(bookingId) } returns current
        stubAssignedAndLookups()
        every { checkInRepository.findByBookingId(bookingId) } returns null
        val saved = slot<BookingAccessCheckIn>()
        every { checkInRepository.recordIfAbsent(capture(saved)) } answers { saved.captured }

        val result = sut.validate(command())

        val granted = assertIs<ValidateBookingAccessResult.Granted>(result)
        assertEquals(current.ticketCode(), granted.view.ticketCode)
        assertEquals("Alice Martin", granted.view.clientName)
        assertEquals("Salle Étoile", granted.view.roomName)
        assertEquals(6, granted.view.numberOfPeople)
        assertEquals(serverId, saved.captured.serverId)
        verify(exactly = 1) { checkInRepository.recordIfAbsent(any()) }
    }

    @Test
    fun `refuses a server that is not assigned to the room on that slot`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns
            setOf(UserId(UUID.randomUUID()))

        val result = sut.validate(command())

        assertEquals(ValidateBookingAccessResult.NotAssignedServer, result)
        verify(exactly = 0) { checkInRepository.recordIfAbsent(any()) }
    }

    @Test
    fun `lets an admin validate without being assigned`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { roomRepository.findById(roomId) } returns room
        every { userRepository.findById(clientId) } returns user(clientId, "Alice", "Martin", UserRole.CLIENT)
        every { checkInRepository.findByBookingId(bookingId) } returns null
        val saved = slot<BookingAccessCheckIn>()
        every { checkInRepository.recordIfAbsent(capture(saved)) } answers { saved.captured }

        val result = sut.validate(command(userId = UserId(UUID.randomUUID()), isAdmin = true))

        assertIs<ValidateBookingAccessResult.Granted>(result)
        verify(exactly = 0) { serverShiftRepository.findServerIdsAssignedTo(any(), any(), any()) }
    }

    @Test
    fun `returns BookingNotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        assertEquals(ValidateBookingAccessResult.BookingNotFound, sut.validate(command()))
    }

    @Test
    fun `refuses a booking that is not confirmed`() {
        every { bookingRepository.findById(bookingId) } returns booking(status = BookingStatus.PENDING)
        stubAssignedAndLookups()

        val result = sut.validate(command())

        val refused = assertIs<ValidateBookingAccessResult.NotConfirmed>(result)
        assertEquals(BookingStatus.PENDING, refused.view.status)
        verify(exactly = 0) { checkInRepository.recordIfAbsent(any()) }
    }

    @Test
    fun `refuses a ticket presented more than 30 minutes before the slot`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(
                startAt = Instant.now().plus(Duration.ofMinutes(45)),
                endAt = Instant.now().plus(Duration.ofHours(3)),
            )
        stubAssignedAndLookups()

        val result = sut.validate(command())

        assertIs<ValidateBookingAccessResult.OutsideAdmissionWindow>(result)
        verify(exactly = 0) { checkInRepository.recordIfAbsent(any()) }
    }

    @Test
    fun `accepts a ticket presented within the early arrival tolerance`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(
                startAt = Instant.now().plus(Duration.ofMinutes(20)),
                endAt = Instant.now().plus(Duration.ofHours(3)),
            )
        stubAssignedAndLookups()
        every { checkInRepository.findByBookingId(bookingId) } returns null
        val saved = slot<BookingAccessCheckIn>()
        every { checkInRepository.recordIfAbsent(capture(saved)) } answers { saved.captured }

        assertIs<ValidateBookingAccessResult.Granted>(sut.validate(command()))
    }

    @Test
    fun `refuses a ticket presented after the slot ended`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(
                startAt = Instant.now().minus(Duration.ofHours(4)),
                endAt = Instant.now().minus(Duration.ofMinutes(1)),
            )
        stubAssignedAndLookups()

        assertIs<ValidateBookingAccessResult.OutsideAdmissionWindow>(sut.validate(command()))
    }

    @Test
    fun `reports the original check-in when the ticket was already validated`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        stubAssignedAndLookups()
        val firstCheckIn =
            BookingAccessCheckIn(
                id = BookingAccessCheckInId.generate(),
                bookingId = bookingId,
                serverId = serverId,
                checkedInAt = Instant.now().minus(Duration.ofMinutes(5)),
            )
        every { checkInRepository.findByBookingId(bookingId) } returns firstCheckIn

        val result = sut.validate(command())

        val already = assertIs<ValidateBookingAccessResult.AlreadyCheckedIn>(result)
        assertEquals(firstCheckIn.checkedInAt, already.firstCheckedInAt)
        assertEquals("Bob Durand", already.checkedInByName)
        verify(exactly = 0) { checkInRepository.recordIfAbsent(any()) }
    }

    @Test
    fun `reports the winning check-in when a concurrent validation got there first`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        stubAssignedAndLookups()
        val winner =
            BookingAccessCheckIn(
                id = BookingAccessCheckInId.generate(),
                bookingId = bookingId,
                serverId = serverId,
                checkedInAt = Instant.now().minus(Duration.ofSeconds(2)),
            )
        every { checkInRepository.findByBookingId(bookingId) } returns null
        every { checkInRepository.recordIfAbsent(any()) } returns winner

        val result = sut.validate(command())

        val already = assertIs<ValidateBookingAccessResult.AlreadyCheckedIn>(result)
        assertEquals(winner.checkedInAt, already.firstCheckedInAt)
    }

    @Test
    fun `reports the original check-in even once the slot has ended`() {
        every { bookingRepository.findById(bookingId) } returns
            booking(
                startAt = Instant.now().minus(Duration.ofHours(5)),
                endAt = Instant.now().minus(Duration.ofHours(1)),
            )
        stubAssignedAndLookups()
        val firstCheckIn =
            BookingAccessCheckIn(
                id = BookingAccessCheckInId.generate(),
                bookingId = bookingId,
                serverId = serverId,
                checkedInAt = Instant.now().minus(Duration.ofHours(4)),
            )
        every { checkInRepository.findByBookingId(bookingId) } returns firstCheckIn

        val result = sut.validate(command())

        val already = assertIs<ValidateBookingAccessResult.AlreadyCheckedIn>(result)
        assertEquals(firstCheckIn.checkedInAt, already.firstCheckedInAt)
        assertEquals("Bob Durand", already.checkedInByName)
    }

    @Test
    fun `returns RoomNotFound when the room referenced by the booking is gone`() {
        every { bookingRepository.findById(bookingId) } returns booking()
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns setOf(serverId)
        every { roomRepository.findById(roomId) } returns null

        assertEquals(ValidateBookingAccessResult.RoomNotFound, sut.validate(command()))
    }
}

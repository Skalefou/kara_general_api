package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftCommand
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
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

class CreateServerShiftServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = CreateServerShiftService(userRepository, roomRepository, serverShiftRepository)

    private val serverId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")
    private val end = start.plusSeconds(5 * 3600)

    private fun user(role: UserRole = UserRole.SERVER): User =
        User(
            id = serverId,
            email = Email("server@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = role,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
        )

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

    private fun command(startAt: Instant = start, endAt: Instant = end) =
        CreateServerShiftCommand(
            serverId = serverId,
            roomId = roomId,
            startAt = startAt,
            endAt = endAt,
            note = "Accueil",
        )

    @Test
    fun `should return ServerNotFound when the server does not exist`() {
        every { userRepository.findById(serverId) } returns null

        val result = sut.createServerShift(command())

        assertEquals(CreateServerShiftResult.ServerNotFound, result)
        verify(exactly = 0) { serverShiftRepository.save(any()) }
    }

    @Test
    fun `should return NotAServer when the account is not a server`() {
        every { userRepository.findById(serverId) } returns user(role = UserRole.CLIENT)

        val result = sut.createServerShift(command())

        assertEquals(CreateServerShiftResult.NotAServer, result)
        verify(exactly = 0) { serverShiftRepository.save(any()) }
    }

    @Test
    fun `should return RoomNotFound when the room does not exist`() {
        every { userRepository.findById(serverId) } returns user()
        every { roomRepository.findById(roomId) } returns null

        val result = sut.createServerShift(command())

        assertEquals(CreateServerShiftResult.RoomNotFound, result)
    }

    @Test
    fun `should return InvalidTimeSlot when end is not after start`() {
        every { userRepository.findById(serverId) } returns user()
        every { roomRepository.findById(roomId) } returns room

        val result = sut.createServerShift(command(startAt = start, endAt = start))

        assertEquals(CreateServerShiftResult.InvalidTimeSlot, result)
    }

    @Test
    fun `should return SlotUnavailable when the shift overlaps another shift of the server`() {
        every { userRepository.findById(serverId) } returns user()
        every { roomRepository.findById(roomId) } returns room
        every { serverShiftRepository.existsOverlappingForServer(serverId, start, end, null) } returns true

        val result = sut.createServerShift(command())

        assertEquals(CreateServerShiftResult.SlotUnavailable, result)
        verify(exactly = 0) { serverShiftRepository.save(any()) }
    }

    @Test
    fun `should persist the shift when everything is valid`() {
        every { userRepository.findById(serverId) } returns user()
        every { roomRepository.findById(roomId) } returns room
        every { serverShiftRepository.existsOverlappingForServer(serverId, start, end, null) } returns false
        val saved = slot<ServerShift>()
        every { serverShiftRepository.save(capture(saved)) } answers { saved.captured }

        val result = sut.createServerShift(command())

        val created = assertIs<CreateServerShiftResult.Created>(result)
        assertEquals(serverId, created.shift.serverId)
        assertEquals(roomId, created.shift.roomId)
        assertEquals(start, created.shift.startAt)
        assertEquals(end, created.shift.endAt)
        assertEquals("Accueil", created.shift.note)
    }
}

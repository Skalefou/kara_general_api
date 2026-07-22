package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftCommand
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
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

class UpdateServerShiftServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = UpdateServerShiftService(roomRepository, serverShiftRepository)

    private val shiftId = ServerShiftId(UUID.randomUUID())
    private val serverId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")
    private val end = start.plusSeconds(5 * 3600)

    private val existing =
        ServerShift(
            id = shiftId,
            serverId = serverId,
            roomId = roomId,
            startAt = start,
            endAt = end,
            note = "Accueil",
            createdAt = Instant.now(),
        )

    private fun room(id: RoomId) =
        Room(
            id = id,
            name = "Salle",
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

    @Test
    fun `should return NotFound when the shift does not exist`() {
        every { serverShiftRepository.findById(shiftId) } returns null

        val result = sut.updateServerShift(UpdateServerShiftCommand(id = shiftId, roomId = null, startAt = null, endAt = null, note = null))

        assertEquals(UpdateServerShiftResult.NotFound, result)
    }

    @Test
    fun `should return RoomNotFound when the new room does not exist`() {
        val newRoom = RoomId(UUID.randomUUID())
        every { serverShiftRepository.findById(shiftId) } returns existing
        every { roomRepository.findById(newRoom) } returns null

        val result = sut.updateServerShift(UpdateServerShiftCommand(id = shiftId, roomId = newRoom, startAt = null, endAt = null, note = null))

        assertEquals(UpdateServerShiftResult.RoomNotFound, result)
    }

    @Test
    fun `should return InvalidTimeSlot when the resulting slot is empty`() {
        every { serverShiftRepository.findById(shiftId) } returns existing

        val result = sut.updateServerShift(UpdateServerShiftCommand(id = shiftId, roomId = null, startAt = null, endAt = start, note = null))

        assertEquals(UpdateServerShiftResult.InvalidTimeSlot, result)
    }

    @Test
    fun `should return SlotUnavailable when the new slot overlaps another shift`() {
        val newEnd = end.plusSeconds(3600)
        every { serverShiftRepository.findById(shiftId) } returns existing
        every { serverShiftRepository.existsOverlappingForServer(serverId, start, newEnd, shiftId) } returns true

        val result = sut.updateServerShift(UpdateServerShiftCommand(id = shiftId, roomId = null, startAt = null, endAt = newEnd, note = null))

        assertEquals(UpdateServerShiftResult.SlotUnavailable, result)
    }

    @Test
    fun `should clear the note when clearNote is set`() {
        every { serverShiftRepository.findById(shiftId) } returns existing
        every { serverShiftRepository.existsOverlappingForServer(serverId, start, end, shiftId) } returns false
        val saved = slot<ServerShift>()
        every { serverShiftRepository.save(capture(saved)) } answers { saved.captured }

        val result = sut.updateServerShift(UpdateServerShiftCommand(id = shiftId, roomId = null, startAt = null, endAt = null, note = null, clearNote = true))

        val success = assertIs<UpdateServerShiftResult.Success>(result)
        assertEquals(null, success.shift.note)
    }

    @Test
    fun `should apply the new room and times`() {
        val newRoom = RoomId(UUID.randomUUID())
        val newStart = start.plusSeconds(3600)
        val newEnd = end.plusSeconds(3600)
        every { serverShiftRepository.findById(shiftId) } returns existing
        every { roomRepository.findById(newRoom) } returns room(newRoom)
        every { serverShiftRepository.existsOverlappingForServer(serverId, newStart, newEnd, shiftId) } returns false
        val saved = slot<ServerShift>()
        every { serverShiftRepository.save(capture(saved)) } answers { saved.captured }

        val result = sut.updateServerShift(UpdateServerShiftCommand(id = shiftId, roomId = newRoom, startAt = newStart, endAt = newEnd, note = null))

        val success = assertIs<UpdateServerShiftResult.Success>(result)
        assertEquals(newRoom, success.shift.roomId)
        assertEquals(newStart, success.shift.startAt)
        assertEquals(newEnd, success.shift.endAt)
        assertEquals("Accueil", success.shift.note)
        verify { serverShiftRepository.save(any()) }
    }
}

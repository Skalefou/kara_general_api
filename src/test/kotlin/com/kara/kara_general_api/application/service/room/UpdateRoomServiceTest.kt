package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomStatus
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateRoomServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val sut = UpdateRoomService(roomRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val existingRoom =
        Room(
            id = roomId,
            name = "Salle Étoile",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            createdAt = Instant.now(),
        )
    private val command =
        UpdateRoomCommand(
            id = roomId,
            name = "Salle Lune",
            street = "5 avenue Foch",
            city = "Lyon",
            postalCode = "69000",
            country = "France",
            status = null,
        )

    @Test
    fun `should update and persist room when it exists`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val result = sut.updateRoom(command)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals("Salle Lune", success.room.name)
        assertEquals(
            Address(street = "5 avenue Foch", city = "Lyon", postalCode = "69000", country = "France"),
            success.room.address,
        )
        assertEquals(roomId, success.room.id)
    }

    @Test
    fun `should keep existing values for fields not provided`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val partialCommand =
            UpdateRoomCommand(
                id = roomId,
                name = "Salle Lune",
                street = null,
                city = null,
                postalCode = null,
                country = null,
                status = null,
            )

        val result = sut.updateRoom(partialCommand)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals("Salle Lune", success.room.name)
        assertEquals(existingRoom.address, success.room.address)
        assertEquals(existingRoom.status, success.room.status)
    }

    @Test
    fun `should close room when status is CLOSED`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val closeCommand =
            UpdateRoomCommand(
                id = roomId,
                name = null,
                street = null,
                city = null,
                postalCode = null,
                country = null,
                status = RoomStatus.CLOSED,
            )

        val result = sut.updateRoom(closeCommand)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals(RoomStatus.CLOSED, success.room.status)
        assertEquals(existingRoom.name, success.room.name)
        assertEquals(existingRoom.address, success.room.address)
    }

    @Test
    fun `should return NotFound when room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.updateRoom(command)

        assertEquals(UpdateRoomResult.NotFound, result)
    }
}

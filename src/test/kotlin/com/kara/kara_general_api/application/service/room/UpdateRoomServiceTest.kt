package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
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
    private val newAddress = Address(street = "5 avenue Foch", city = "Lyon", postalCode = "69000", country = "France")
    private val command = UpdateRoomCommand(id = roomId, name = "Salle Lune", address = newAddress)

    @Test
    fun `should update and persist room when it exists`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val result = sut.updateRoom(command)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals("Salle Lune", success.room.name)
        assertEquals(newAddress, success.room.address)
        assertEquals(roomId, success.room.id)
    }

    @Test
    fun `should return NotFound when room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.updateRoom(command)

        assertEquals(UpdateRoomResult.NotFound, result)
    }
}

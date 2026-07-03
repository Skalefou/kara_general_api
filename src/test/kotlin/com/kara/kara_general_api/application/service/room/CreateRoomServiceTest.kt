package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CreateRoomServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val sut = CreateRoomService(roomRepository)

    private val address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France")
    private val command = CreateRoomCommand(name = "Salle Étoile", address = address)

    @Test
    fun `should create and persist room`() {
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val room = sut.createRoom(command)

        assertEquals("Salle Étoile", room.name)
        assertEquals(address, room.address)
        verify { roomRepository.save(room) }
    }
}

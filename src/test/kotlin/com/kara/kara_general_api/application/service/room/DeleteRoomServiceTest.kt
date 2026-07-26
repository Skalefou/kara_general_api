package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class DeleteRoomServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val sut = DeleteRoomService(roomRepository)

    private val roomId = RoomId(UUID.randomUUID())

    @Test
    fun `should return Success when room is deleted`() {
        every { roomRepository.deleteById(roomId) } returns true

        val result = sut.deleteRoom(roomId)

        assertEquals(DeleteRoomResult.Success, result)
    }

    @Test
    fun `should return NotFound when room does not exist`() {
        every { roomRepository.deleteById(roomId) } returns false

        val result = sut.deleteRoom(roomId)

        assertEquals(DeleteRoomResult.NotFound, result)
    }
}

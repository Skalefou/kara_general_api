package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class GetRoomServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val sut = GetRoomService(roomRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            pricePerPersonPerHour = BigDecimal("12.50"),
            createdAt = Instant.now(),
        )

    @Test
    fun `should return Success when room exists`() {
        every { roomRepository.findById(roomId) } returns room

        val result = sut.getRoom(roomId)

        assertEquals(GetRoomResult.Success(room), result)
    }

    @Test
    fun `should return NotFound when room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.getRoom(roomId)

        assertEquals(GetRoomResult.NotFound, result)
    }
}

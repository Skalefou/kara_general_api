package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
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
    private val roomOptionRepository = mockk<RoomOptionRepository>()
    private val sut = GetRoomService(roomRepository, roomOptionRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            description = "Grande salle lumineuse",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            pricePerPersonPerHour = BigDecimal("12.50"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
            createdAt = Instant.now(),
        )

    @Test
    fun `should return Success with the room and its options when room exists`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns emptyList()

        val result = sut.getRoom(roomId)

        assertEquals(GetRoomResult.Success(room, emptyList()), result)
    }

    @Test
    fun `should return NotFound when room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.getRoom(roomId)

        assertEquals(GetRoomResult.NotFound, result)
    }
}

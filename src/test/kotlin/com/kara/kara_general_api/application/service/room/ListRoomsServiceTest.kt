package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ListRoomsServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListRoomsService(roomRepository)

    private val room =
        Room(
            id = RoomId(UUID.randomUUID()),
            name = "Salle Étoile",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            createdAt = Instant.now(),
        )

    @Test
    fun `should return page of rooms with total count`() {
        every { roomRepository.findAll(page = 0, size = 20) } returns listOf(room)
        every { roomRepository.count() } returns 1L

        val result = sut.listRooms(ListRoomsQuery(page = 0, size = 20))

        assertEquals(listOf(room), result.rooms)
        assertEquals(0, result.page)
        assertEquals(20, result.size)
        assertEquals(1L, result.totalElements)
    }
}

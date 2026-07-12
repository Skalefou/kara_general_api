package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.BoundingBox
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListRoomsServiceTest {

    private val maxResults = 200
    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListRoomsService(roomRepository, maxResults)

    private val paris = BoundingBox(minLat = 48.8, minLng = 2.2, maxLat = 48.9, maxLng = 2.4)

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
        assertNull(result.totalInBbox)
        assertNull(result.truncated)
        verify(exactly = 0) { roomRepository.findInBbox(any(), any()) }
    }

    @Test
    fun `should filter by bbox and cap results without truncation flag when under the cap`() {
        every { roomRepository.countInBbox(paris) } returns 3L
        every { roomRepository.findInBbox(paris, maxResults) } returns listOf(room)

        val result = sut.listRooms(ListRoomsQuery(bbox = paris))

        assertEquals(listOf(room), result.rooms)
        assertEquals(3L, result.totalInBbox)
        assertEquals(3L, result.totalElements)
        assertFalse(result.truncated!!)
        verify(exactly = 0) { roomRepository.findAll(any(), any()) }
    }

    @Test
    fun `should flag truncated when bbox count exceeds the cap`() {
        every { roomRepository.countInBbox(paris) } returns 250L
        every { roomRepository.findInBbox(paris, maxResults) } returns List(maxResults) { room }

        val result = sut.listRooms(ListRoomsQuery(bbox = paris))

        assertEquals(maxResults, result.rooms.size)
        assertEquals(250L, result.totalInBbox)
        assertTrue(result.truncated!!)
    }
}

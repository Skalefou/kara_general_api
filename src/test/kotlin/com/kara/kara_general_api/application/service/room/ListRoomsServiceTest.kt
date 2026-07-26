package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomCluster
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.BoundingBox
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.input.room.ViewportMode
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListRoomsServiceTest {
    private val maxResults = 200
    private val gridSize = 8
    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListRoomsService(roomRepository, maxResults, gridSize)

    private val paris = BoundingBox(minLat = 48.8, minLng = 2.2, maxLat = 48.9, maxLng = 2.4)

    private val room =
        Room(
            id = RoomId(UUID.randomUUID()),
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
    fun `should return rooms mode with empty clusters when count is under the cap`() {
        every { roomRepository.countInBbox(paris) } returns 3L
        every { roomRepository.findInBbox(paris, maxResults) } returns listOf(room)

        val result = sut.listRooms(ListRoomsQuery(bbox = paris))

        assertEquals(ViewportMode.ROOMS, result.mode)
        assertEquals(listOf(room), result.rooms)
        assertEquals(emptyList(), result.clusters)
        assertEquals(3L, result.totalInBbox)
        assertEquals(3L, result.totalElements)
        assertFalse(result.truncated!!)
        verify(exactly = 0) { roomRepository.findAll(any(), any()) }
        verify(exactly = 0) { roomRepository.clustersInBbox(any(), any()) }
    }

    @Test
    fun `should switch to clusters mode when count exceeds the cap`() {
        val clusters =
            listOf(
                RoomCluster(latitude = 48.86, longitude = 2.34, count = 150),
                RoomCluster(latitude = 48.89, longitude = 2.24, count = 100),
            )
        every { roomRepository.countInBbox(paris) } returns 250L
        every { roomRepository.clustersInBbox(paris, gridSize) } returns clusters

        val result = sut.listRooms(ListRoomsQuery(bbox = paris))

        assertEquals(ViewportMode.CLUSTERS, result.mode)
        assertTrue(result.rooms.isEmpty())
        assertEquals(clusters, result.clusters)
        assertEquals(250L, result.totalInBbox)
        assertFalse(result.truncated!!)
        assertEquals(250L, result.clusters!!.sumOf { it.count })
        verify(exactly = 0) { roomRepository.findInBbox(any(), any()) }
    }
}

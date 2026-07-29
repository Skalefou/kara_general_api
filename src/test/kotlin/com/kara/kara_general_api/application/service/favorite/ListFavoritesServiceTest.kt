package com.kara.kara_general_api.application.service.favorite

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesQuery
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ListFavoritesServiceTest {
    private val roomFavoriteRepository = mockk<RoomFavoriteRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListFavoritesService(roomFavoriteRepository, roomRepository)

    private val userId = UserId(UUID.randomUUID())
    private val firstRoomId = RoomId(UUID.randomUUID())
    private val secondRoomId = RoomId(UUID.randomUUID())

    private fun room(
        id: RoomId,
        name: String,
    ) = Room(
        id = id,
        name = name,
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
    fun `should return the favorite rooms in the favorite order`() {
        val first = room(firstRoomId, "Salle Étoile")
        val second = room(secondRoomId, "Salle Nation")
        every { roomFavoriteRepository.findRoomIdsByUser(userId, 0, 20) } returns listOf(firstRoomId, secondRoomId)
        every { roomRepository.findByIds(listOf(firstRoomId, secondRoomId)) } returns listOf(second, first)
        every { roomFavoriteRepository.countByUser(userId) } returns 2

        val page = sut.listFavorites(ListFavoritesQuery(userId = userId))

        assertEquals(listOf(first, second), page.rooms)
        assertEquals(2, page.totalElements)
    }

    @Test
    fun `should skip favorites whose room no longer exists`() {
        val first = room(firstRoomId, "Salle Étoile")
        every { roomFavoriteRepository.findRoomIdsByUser(userId, 0, 20) } returns listOf(firstRoomId, secondRoomId)
        every { roomRepository.findByIds(listOf(firstRoomId, secondRoomId)) } returns listOf(first)
        every { roomFavoriteRepository.countByUser(userId) } returns 2

        val page = sut.listFavorites(ListFavoritesQuery(userId = userId))

        assertEquals(listOf(first), page.rooms)
    }

    @Test
    fun `should return an empty page when the user has no favorite`() {
        every { roomFavoriteRepository.findRoomIdsByUser(userId, 0, 20) } returns emptyList()
        every { roomRepository.findByIds(emptyList()) } returns emptyList()
        every { roomFavoriteRepository.countByUser(userId) } returns 0

        val page = sut.listFavorites(ListFavoritesQuery(userId = userId))

        assertEquals(emptyList(), page.rooms)
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `should return every favorite room id without pagination`() {
        every { roomFavoriteRepository.findAllRoomIdsByUser(userId) } returns listOf(firstRoomId, secondRoomId)

        val ids = sut.listFavoriteRoomIds(userId)

        assertEquals(listOf(firstRoomId, secondRoomId), ids)
    }
}

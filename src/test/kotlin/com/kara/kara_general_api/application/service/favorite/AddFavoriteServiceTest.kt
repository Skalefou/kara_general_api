package com.kara.kara_general_api.application.service.favorite

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteResult
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class AddFavoriteServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val roomFavoriteRepository = mockk<RoomFavoriteRepository>(relaxed = true)
    private val sut = AddFavoriteService(roomRepository, roomFavoriteRepository)

    private val userId = UserId(UUID.randomUUID())
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
    fun `should return Success and persist the favorite when the room exists`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomFavoriteRepository.add(userId, roomId) } returns true

        val result = sut.addFavorite(AddFavoriteCommand(userId, roomId))

        assertEquals(AddFavoriteResult.Success, result)
        verify(exactly = 1) { roomFavoriteRepository.add(userId, roomId) }
    }

    @Test
    fun `should return Success when the room is already a favorite`() {
        every { roomRepository.findById(roomId) } returns room
        every { roomFavoriteRepository.add(userId, roomId) } returns false

        val result = sut.addFavorite(AddFavoriteCommand(userId, roomId))

        assertEquals(AddFavoriteResult.Success, result)
    }

    @Test
    fun `should return RoomNotFound and persist nothing when the room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.addFavorite(AddFavoriteCommand(userId, roomId))

        assertEquals(AddFavoriteResult.RoomNotFound, result)
        verify(exactly = 0) { roomFavoriteRepository.add(any(), any()) }
    }
}

package com.kara.kara_general_api.application.service.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteResult
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class RemoveFavoriteServiceTest {
    private val roomFavoriteRepository = mockk<RoomFavoriteRepository>()
    private val sut = RemoveFavoriteService(roomFavoriteRepository)

    private val userId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())

    @Test
    fun `should return Success when a favorite was deleted`() {
        every { roomFavoriteRepository.remove(userId, roomId) } returns true

        val result = sut.removeFavorite(RemoveFavoriteCommand(userId, roomId))

        assertEquals(RemoveFavoriteResult.Success, result)
    }

    @Test
    fun `should return NotFound when the room was not a favorite`() {
        every { roomFavoriteRepository.remove(userId, roomId) } returns false

        val result = sut.removeFavorite(RemoveFavoriteCommand(userId, roomId))

        assertEquals(RemoveFavoriteResult.NotFound, result)
    }
}

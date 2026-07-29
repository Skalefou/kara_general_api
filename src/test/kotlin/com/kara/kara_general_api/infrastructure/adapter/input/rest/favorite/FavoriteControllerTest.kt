package com.kara.kara_general_api.infrastructure.adapter.input.rest.favorite

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteResult
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteUseCase
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoriteRoomIdsUseCase
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesQuery
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesUseCase
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteResult
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteUseCase
import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

private const val ROOM_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val USER_ID = "11111111-2222-3333-4444-555555555555"

@WebMvcTest(FavoriteController::class)
@Import(SecurityConfig::class)
class FavoriteControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var listFavoritesUseCase: ListFavoritesUseCase

    @MockkBean
    private lateinit var listFavoriteRoomIdsUseCase: ListFavoriteRoomIdsUseCase

    @MockkBean
    private lateinit var addFavoriteUseCase: AddFavoriteUseCase

    @MockkBean
    private lateinit var removeFavoriteUseCase: RemoveFavoriteUseCase

    @MockkBean
    private lateinit var imageStorage: ImageStoragePort

    private val room =
        Room(
            id = RoomId(UUID.fromString(ROOM_ID)),
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
    fun `should return 401 when listing favorites without authentication`() {
        mockMvc.perform(get("/api/v1/users/me/favorites")).andExpect(status().isUnauthorized)

        verify(exactly = 0) { listFavoritesUseCase.listFavorites(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the favorite rooms of the caller`() {
        val query = slot<ListFavoritesQuery>()
        every { listFavoritesUseCase.listFavorites(capture(query)) } returns
            RoomPage(rooms = listOf(room), page = 0, size = 20, totalElements = 1)

        mockMvc
            .perform(get("/api/v1/users/me/favorites"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms[0].id").value(ROOM_ID))
            .andExpect(jsonPath("$.totalElements").value(1))

        assertEquals(UserId(UUID.fromString(USER_ID)), query.captured.userId)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with every favorite room id`() {
        every { listFavoriteRoomIdsUseCase.listFavoriteRoomIds(UserId(UUID.fromString(USER_ID))) } returns
            listOf(RoomId(UUID.fromString(ROOM_ID)))

        mockMvc
            .perform(get("/api/v1/users/me/favorites/ids"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roomIds[0]").value(ROOM_ID))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 204 when adding a room to the favorites`() {
        val command = slot<AddFavoriteCommand>()
        every { addFavoriteUseCase.addFavorite(capture(command)) } returns AddFavoriteResult.Success

        mockMvc.perform(put("/api/v1/users/me/favorites/$ROOM_ID")).andExpect(status().isNoContent)

        assertEquals(UserId(UUID.fromString(USER_ID)), command.captured.userId)
        assertEquals(RoomId(UUID.fromString(ROOM_ID)), command.captured.roomId)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when adding an unknown room to the favorites`() {
        every { addFavoriteUseCase.addFavorite(any()) } returns AddFavoriteResult.RoomNotFound

        mockMvc
            .perform(put("/api/v1/users/me/favorites/$ROOM_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 204 when removing a room from the favorites`() {
        val command = slot<RemoveFavoriteCommand>()
        every { removeFavoriteUseCase.removeFavorite(capture(command)) } returns RemoveFavoriteResult.Success

        mockMvc.perform(delete("/api/v1/users/me/favorites/$ROOM_ID")).andExpect(status().isNoContent)

        assertEquals(RoomId(UUID.fromString(ROOM_ID)), command.captured.roomId)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when removing a room that is not a favorite`() {
        every { removeFavoriteUseCase.removeFavorite(any()) } returns RemoveFavoriteResult.NotFound

        mockMvc
            .perform(delete("/api/v1/users/me/favorites/$ROOM_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("FAVORITE_NOT_FOUND"))
    }
}

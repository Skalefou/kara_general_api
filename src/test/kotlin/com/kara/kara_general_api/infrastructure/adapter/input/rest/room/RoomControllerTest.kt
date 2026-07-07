package com.kara.kara_general_api.infrastructure.adapter.input.rest.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomResult
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.input.room.GetRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.ListRoomsUseCase
import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

private const val ROOM_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val REQUEST_BODY =
    """{"name": "Salle Étoile", "street": "12 rue de la Paix", "city": "Paris", "postalCode": "75002", "country": "France"}"""

@WebMvcTest(RoomController::class)
@Import(SecurityConfig::class)
class RoomControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createRoomUseCase: CreateRoomUseCase

    @MockkBean
    private lateinit var getRoomUseCase: GetRoomUseCase

    @MockkBean
    private lateinit var listRoomsUseCase: ListRoomsUseCase

    @MockkBean
    private lateinit var updateRoomUseCase: UpdateRoomUseCase

    @MockkBean
    private lateinit var deleteRoomUseCase: DeleteRoomUseCase

    private val room =
        Room(
            id = RoomId(UUID.fromString(ROOM_ID)),
            name = "Salle Étoile",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            createdAt = Instant.now(),
        )

    @Test
    fun `should return 200 with room list without authentication`() {
        every { listRoomsUseCase.listRooms(any()) } returns
            RoomPage(rooms = listOf(room), page = 0, size = 20, totalElements = 1)

        mockMvc.perform(get("/api/v1/rooms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `should return 200 with room without authentication`() {
        every { getRoomUseCase.getRoom(room.id) } returns GetRoomResult.Success(room)

        mockMvc.perform(get("/api/v1/rooms/$ROOM_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Salle Étoile"))
    }

    @Test
    fun `should return 404 when room is not found`() {
        every { getRoomUseCase.getRoom(room.id) } returns GetRoomResult.NotFound

        mockMvc.perform(get("/api/v1/rooms/$ROOM_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 201 when admin creates a room`() {
        every { createRoomUseCase.createRoom(any()) } returns room

        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isCreated)
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin creates a room`() {
        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated creates a room`() {
        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 when admin updates a room`() {
        every { updateRoomUseCase.updateRoom(any()) } returns UpdateRoomResult.Success(room)

        mockMvc.perform(
            patch("/api/v1/rooms/$ROOM_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin updates an unknown room`() {
        every { updateRoomUseCase.updateRoom(any()) } returns UpdateRoomResult.NotFound

        mockMvc.perform(
            patch("/api/v1/rooms/$ROOM_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 when admin updates a room with a partial body`() {
        every { updateRoomUseCase.updateRoom(any()) } returns UpdateRoomResult.Success(room)

        mockMvc.perform(
            patch("/api/v1/rooms/$ROOM_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "Salle Lune"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 when admin closes a room via status`() {
        every { updateRoomUseCase.updateRoom(any()) } returns UpdateRoomResult.Success(room)

        mockMvc.perform(
            patch("/api/v1/rooms/$ROOM_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "CLOSED"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin updates a room`() {
        mockMvc.perform(
            patch("/api/v1/rooms/$ROOM_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 204 when admin deletes a room`() {
        every { deleteRoomUseCase.deleteRoom(room.id) } returns DeleteRoomResult.Success

        mockMvc.perform(delete("/api/v1/rooms/$ROOM_ID"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin deletes an unknown room`() {
        every { deleteRoomUseCase.deleteRoom(room.id) } returns DeleteRoomResult.NotFound

        mockMvc.perform(delete("/api/v1/rooms/$ROOM_ID"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin deletes a room`() {
        mockMvc.perform(delete("/api/v1/rooms/$ROOM_ID"))
            .andExpect(status().isForbidden)
    }
}

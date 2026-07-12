package com.kara.kara_general_api.infrastructure.adapter.input.rest.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomCluster
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageUseCase
import com.kara.kara_general_api.domain.port.input.room.CreateRoomResult
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomResult
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.input.room.GetRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.input.room.ListRoomsUseCase
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageUseCase
import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.input.room.ViewportMode
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
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

    @MockkBean
    private lateinit var addRoomImageUseCase: AddRoomImageUseCase

    @MockkBean
    private lateinit var removeRoomImageUseCase: RemoveRoomImageUseCase

    @MockkBean
    private lateinit var imageStorage: ImageStoragePort

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
    fun `should list without bbox when no viewport param is provided`() {
        val querySlot = slot<ListRoomsQuery>()
        every { listRoomsUseCase.listRooms(capture(querySlot)) } returns
            RoomPage(rooms = listOf(room), page = 0, size = 20, totalElements = 1)

        mockMvc.perform(get("/api/v1/rooms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalInBbox").doesNotExist())
            .andExpect(jsonPath("$.truncated").doesNotExist())

        assertNull(querySlot.captured.bbox)
    }

    @Test
    fun `should list within bbox in rooms mode`() {
        val querySlot = slot<ListRoomsQuery>()
        every { listRoomsUseCase.listRooms(capture(querySlot)) } returns
            RoomPage(
                rooms = listOf(room),
                page = 0,
                size = 20,
                totalElements = 3,
                totalInBbox = 3,
                truncated = false,
                mode = ViewportMode.ROOMS,
                clusters = emptyList(),
            )

        mockMvc.perform(
            get("/api/v1/rooms")
                .param("minLat", "48.8")
                .param("minLng", "2.2")
                .param("maxLat", "48.9")
                .param("maxLng", "2.4"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.mode").value("rooms"))
            .andExpect(jsonPath("$.totalInBbox").value(3))
            .andExpect(jsonPath("$.truncated").value(false))
            .andExpect(jsonPath("$.rooms.length()").value(1))
            .andExpect(jsonPath("$.clusters.length()").value(0))

        assertNotNull(querySlot.captured.bbox)
    }

    @Test
    fun `should list within bbox in clusters mode`() {
        every { listRoomsUseCase.listRooms(any()) } returns
            RoomPage(
                rooms = emptyList(),
                page = 0,
                size = 20,
                totalElements = 640,
                totalInBbox = 640,
                truncated = false,
                mode = ViewportMode.CLUSTERS,
                clusters =
                    listOf(
                        RoomCluster(latitude = 48.86, longitude = 2.34, count = 420),
                        RoomCluster(latitude = 48.89, longitude = 2.24, count = 220),
                    ),
            )

        mockMvc.perform(
            get("/api/v1/rooms")
                .param("minLat", "48.5")
                .param("minLng", "1.9")
                .param("maxLat", "49.1")
                .param("maxLng", "2.8"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.mode").value("clusters"))
            .andExpect(jsonPath("$.totalInBbox").value(640))
            .andExpect(jsonPath("$.rooms.length()").value(0))
            .andExpect(jsonPath("$.clusters.length()").value(2))
            .andExpect(jsonPath("$.clusters[0].count").value(420))
            .andExpect(jsonPath("$.clusters[0].latitude").value(48.86))
    }

    @Test
    fun `should return 400 when only some bbox params are provided`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .param("minLat", "48.8")
                .param("minLng", "2.2")
                .param("maxLat", "48.9"),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BBOX_INCOMPLETE"))

        verify(exactly = 0) { listRoomsUseCase.listRooms(any()) }
    }

    @Test
    fun `should return 400 when bbox min latitude exceeds max latitude`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .param("minLat", "49.0")
                .param("minLng", "2.2")
                .param("maxLat", "48.0")
                .param("maxLng", "2.4"),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BBOX_INVALID"))

        verify(exactly = 0) { listRoomsUseCase.listRooms(any()) }
    }

    @Test
    fun `should return 400 when bbox latitude is out of range`() {
        mockMvc.perform(
            get("/api/v1/rooms")
                .param("minLat", "-91.0")
                .param("minLng", "2.2")
                .param("maxLat", "48.9")
                .param("maxLng", "2.4"),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BBOX_INVALID"))

        verify(exactly = 0) { listRoomsUseCase.listRooms(any()) }
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
        every { createRoomUseCase.createRoom(any()) } returns CreateRoomResult.Success(room)

        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isCreated)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 400 when created room address is not geocodable`() {
        every { createRoomUseCase.createRoom(any()) } returns CreateRoomResult.AddressNotFound

        mockMvc.perform(
            post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("ADDRESS_NOT_GEOCODABLE"))
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
    fun `should return 400 when updated room address is not geocodable`() {
        every { updateRoomUseCase.updateRoom(any()) } returns UpdateRoomResult.AddressNotFound

        mockMvc.perform(
            patch("/api/v1/rooms/$ROOM_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("ADDRESS_NOT_GEOCODABLE"))
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

    private fun imageFile(contentType: String = MediaType.IMAGE_JPEG_VALUE) =
        MockMultipartFile("file", "room.jpg", contentType, byteArrayOf(1, 2, 3))

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 201 with the public url when admin adds a room image`() {
        val image = RoomImage(RoomImageId(UUID.randomUUID()), "rooms/$ROOM_ID/img.jpg", 0)
        every { addRoomImageUseCase.addImage(any()) } returns
            AddRoomImageResult.Success(image, "https://cdn.example/rooms/$ROOM_ID/img.jpg")

        mockMvc.perform(multipart("/api/v1/rooms/$ROOM_ID/images").file(imageFile()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.url").value("https://cdn.example/rooms/$ROOM_ID/img.jpg"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 415 when the room image type is not supported`() {
        every { addRoomImageUseCase.addImage(any()) } returns AddRoomImageResult.InvalidImageType

        mockMvc.perform(multipart("/api/v1/rooms/$ROOM_ID/images").file(imageFile("text/plain")))
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE_TYPE"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin adds a room image`() {
        mockMvc.perform(multipart("/api/v1/rooms/$ROOM_ID/images").file(imageFile()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated adds a room image`() {
        mockMvc.perform(multipart("/api/v1/rooms/$ROOM_ID/images").file(imageFile()))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 204 when admin removes a room image`() {
        every { removeRoomImageUseCase.removeImage(any()) } returns RemoveRoomImageResult.Success

        mockMvc.perform(delete("/api/v1/rooms/$ROOM_ID/images/${UUID.randomUUID()}"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin removes an unknown room image`() {
        every { removeRoomImageUseCase.removeImage(any()) } returns RemoveRoomImageResult.ImageNotFound

        mockMvc.perform(delete("/api/v1/rooms/$ROOM_ID/images/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ROOM_IMAGE_NOT_FOUND"))
    }
}

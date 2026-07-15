package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageResult
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddRoomImageServiceTest {

    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val imageStorage = mockk<ImageStoragePort>(relaxed = true)
    private val sut = AddRoomImageService(roomRepository, imageStorage)

    private val roomId = RoomId(UUID.randomUUID())
    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            address = Address("12 rue de la Paix", "Paris", "75002", "France"),
            pricePerPersonPerHour = BigDecimal("12.50"),
            createdAt = Instant.now(),
        )

    @Test
    fun `should reject an unsupported content type`() {
        val result = sut.addImage(AddRoomImageCommand(roomId, byteArrayOf(1), "application/pdf"))

        assertIs<AddRoomImageResult.InvalidImageType>(result)
        verify(exactly = 0) { roomRepository.findById(any()) }
    }

    @Test
    fun `should return RoomNotFound when the room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.addImage(AddRoomImageCommand(roomId, byteArrayOf(1), "image/jpeg"))

        assertIs<AddRoomImageResult.RoomNotFound>(result)
        verify(exactly = 0) { imageStorage.upload(any(), any(), any(), any()) }
    }

    @Test
    fun `should upload to the public bucket and persist the image at the next position`() {
        val existing = RoomImage(RoomImageId.generate(), "rooms/x.jpg", 0)
        every { roomRepository.findById(roomId) } returns room.copy(images = listOf(existing))
        val savedImage = slot<RoomImage>()
        every { roomRepository.addImage(any(), capture(savedImage)) } answers { savedImage.captured }
        every { imageStorage.publicUrl(any()) } returns "https://cdn.example/rooms/new.jpg"

        val result = sut.addImage(AddRoomImageCommand(roomId, byteArrayOf(1, 2, 3), "image/png"))

        val success = assertIs<AddRoomImageResult.Success>(result)
        assertEquals("https://cdn.example/rooms/new.jpg", success.url)
        assertEquals(1, savedImage.captured.position)
        assertTrue(savedImage.captured.objectKey.startsWith("rooms/${roomId.value}/"))
        assertTrue(savedImage.captured.objectKey.endsWith(".png"))
        verify { imageStorage.upload(ImageVisibility.PUBLIC, savedImage.captured.objectKey, any(), "image/png") }
    }
}

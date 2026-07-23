package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.image.ImageJobCorrelation
import com.kara.kara_general_api.domain.model.image.ImageProcessingJob
import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.RoomImageStatus
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageResult
import com.kara.kara_general_api.domain.port.output.ImageJobCorrelationRepository
import com.kara.kara_general_api.domain.port.output.ImageProcessingPort
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
    private val imageProcessing = mockk<ImageProcessingPort>(relaxed = true)
    private val correlationRepository = mockk<ImageJobCorrelationRepository>(relaxed = true)
    private val sut = AddRoomImageService(roomRepository, imageStorage, imageProcessing, correlationRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            description = "Grande salle lumineuse",
            address = Address("12 rue de la Paix", "Paris", "75002", "France"),
            pricePerPersonPerHour = BigDecimal("12.50"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
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
    fun `should upload the private original, persist PROCESSING and enqueue a processing job`() {
        val existing = RoomImage(RoomImageId.generate(), "rooms/x/originals/y.jpg", 0)
        every { roomRepository.findById(roomId) } returns room.copy(images = listOf(existing))
        val savedImage = slot<RoomImage>()
        every { roomRepository.addImage(any(), capture(savedImage)) } answers { savedImage.captured }
        val correlation = slot<ImageJobCorrelation>()
        every { correlationRepository.save(capture(correlation)) } returns Unit
        val job = slot<ImageProcessingJob>()
        every { imageProcessing.enqueue(capture(job)) } returns Unit

        val result = sut.addImage(AddRoomImageCommand(roomId, byteArrayOf(1, 2, 3), "image/png"))

        val accepted = assertIs<AddRoomImageResult.Accepted>(result)
        // Persistance : original privé, statut PROCESSING, position suivante.
        assertEquals(RoomImageStatus.PROCESSING, savedImage.captured.status)
        assertEquals(1, savedImage.captured.position)
        assertEquals(accepted.imageId, savedImage.captured.id.value)
        assertTrue(savedImage.captured.objectKey.startsWith("rooms/${roomId.value}/originals/"))
        assertTrue(savedImage.captured.objectKey.endsWith(".png"))
        verify {
            imageStorage.upload(ImageVisibility.PRIVATE, savedImage.captured.objectKey, any(), "image/png")
        }
        // Corrélation + job cohérents (même jobId, cible ROOM, source = original privé).
        assertEquals(ImageProcessingTarget.ROOM, correlation.captured.target)
        assertEquals(roomId.value, correlation.captured.ownerId)
        assertEquals(accepted.imageId, correlation.captured.imageId)
        assertEquals(correlation.captured.jobId, job.captured.jobId)
        assertEquals(ImageProcessingTarget.ROOM, job.captured.target)
        assertEquals(savedImage.captured.objectKey, job.captured.sourceKey)
        assertEquals("image/png", job.captured.contentType)
    }
}

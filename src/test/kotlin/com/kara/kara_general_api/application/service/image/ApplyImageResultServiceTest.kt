package com.kara.kara_general_api.application.service.image

import com.kara.kara_general_api.domain.model.image.ImageJobCorrelation
import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.model.room.RoomImageVariant
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.image.AppliedImageVariant
import com.kara.kara_general_api.domain.port.input.image.ApplyImageResultCommand
import com.kara.kara_general_api.domain.port.output.ImageJobCorrelationRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class ApplyImageResultServiceTest {
    private val correlationRepository = mockk<ImageJobCorrelationRepository>(relaxed = true)
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val sut = ApplyImageResultService(correlationRepository, roomRepository, userRepository)

    private val jobId = UUID.randomUUID()
    private val imageId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()

    @Test
    fun `should ignore a result whose job id is unknown`() {
        every { correlationRepository.findByJobId(jobId) } returns null

        sut.apply(ApplyImageResultCommand(jobId, success = true))

        verify(exactly = 0) { roomRepository.markImageReady(any(), any()) }
        verify(exactly = 0) { roomRepository.markImageFailed(any(), any()) }
        verify(exactly = 0) { userRepository.markPhotoReady(any(), any(), any()) }
        verify(exactly = 0) { userRepository.markPhotoFailed(any()) }
    }

    @Test
    fun `should mark a room image ready with its variants on success`() {
        every { correlationRepository.findByJobId(jobId) } returns
            ImageJobCorrelation(jobId, ImageProcessingTarget.ROOM, ownerId, imageId)
        val variants = slot<List<RoomImageVariant>>()
        every { roomRepository.markImageReady(imageId, capture(variants)) } returns Unit

        sut.apply(
            ApplyImageResultCommand(
                jobId = jobId,
                success = true,
                variants =
                    listOf(
                        AppliedImageVariant("thumbnail", "rooms/a/b/thumbnail.webp", 320, 320, 100, "image/webp"),
                    ),
            ),
        )

        assertEquals(1, variants.captured.size)
        assertEquals("rooms/a/b/thumbnail.webp", variants.captured.first().objectKey)
    }

    @Test
    fun `should mark a room image failed with the error code on failure`() {
        every { correlationRepository.findByJobId(jobId) } returns
            ImageJobCorrelation(jobId, ImageProcessingTarget.ROOM, ownerId, imageId)

        sut.apply(ApplyImageResultCommand(jobId, success = false, errorCode = "DECODE_FAILED"))

        verify { roomRepository.markImageFailed(imageId, "DECODE_FAILED") }
    }

    @Test
    fun `should mark a profile photo ready with thumbnail and full keys on success`() {
        every { correlationRepository.findByJobId(jobId) } returns
            ImageJobCorrelation(jobId, ImageProcessingTarget.PROFILE, ownerId, imageId)

        sut.apply(
            ApplyImageResultCommand(
                jobId = jobId,
                success = true,
                variants =
                    listOf(
                        AppliedImageVariant("thumbnail", "profiles/a/b/thumbnail.webp", 320, 320, 10, "image/webp"),
                        AppliedImageVariant("full", "profiles/a/b/full.webp", 1024, 1024, 50, "image/webp"),
                    ),
            ),
        )

        verify {
            userRepository.markPhotoReady(UserId(ownerId), "profiles/a/b/thumbnail.webp", "profiles/a/b/full.webp")
        }
    }

    @Test
    fun `should mark a profile photo failed when a successful result misses an expected variant`() {
        every { correlationRepository.findByJobId(jobId) } returns
            ImageJobCorrelation(jobId, ImageProcessingTarget.PROFILE, ownerId, imageId)

        sut.apply(
            ApplyImageResultCommand(
                jobId = jobId,
                success = true,
                variants =
                    listOf(
                        AppliedImageVariant("thumbnail", "profiles/a/b/thumbnail.webp", 320, 320, 10, "image/webp"),
                    ),
            ),
        )

        verify { userRepository.markPhotoFailed(UserId(ownerId)) }
        verify(exactly = 0) { userRepository.markPhotoReady(any(), any(), any()) }
    }

    @Test
    fun `should mark a profile photo failed on worker failure`() {
        every { correlationRepository.findByJobId(jobId) } returns
            ImageJobCorrelation(jobId, ImageProcessingTarget.PROFILE, ownerId, imageId)

        sut.apply(ApplyImageResultCommand(jobId, success = false, errorCode = "TIMEOUT"))

        verify { userRepository.markPhotoFailed(UserId(ownerId)) }
    }
}

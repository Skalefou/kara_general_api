package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoResult
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateProfilePhotoServiceTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val imageStorage = mockk<ImageStoragePort>(relaxed = true)
    private val sut = UpdateProfilePhotoService(userRepository, imageStorage)

    private val userId = UserId(UUID.randomUUID())
    private val user =
        User(
            id = userId,
            email = Email("jane.doe@example.com"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("+33612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
        )

    @Test
    fun `should reject an unsupported content type without touching the repository`() {
        val result =
            sut.updatePhoto(UpdateProfilePhotoCommand(userId, byteArrayOf(1, 2, 3), "text/plain"))

        assertIs<UpdateProfilePhotoResult.InvalidImageType>(result)
        verify(exactly = 0) { userRepository.findById(any()) }
        verify(exactly = 0) { imageStorage.upload(any(), any(), any(), any()) }
    }

    @Test
    fun `should reject an image larger than the maximum size`() {
        val tooBig = ByteArray((5L * 1024 * 1024 + 1).toInt())

        val result = sut.updatePhoto(UpdateProfilePhotoCommand(userId, tooBig, "image/png"))

        assertIs<UpdateProfilePhotoResult.ImageTooLarge>(result)
        verify(exactly = 0) { imageStorage.upload(any(), any(), any(), any()) }
    }

    @Test
    fun `should return UserNotFound when the user does not exist`() {
        every { userRepository.findById(userId) } returns null

        val result = sut.updatePhoto(UpdateProfilePhotoCommand(userId, byteArrayOf(1), "image/jpeg"))

        assertIs<UpdateProfilePhotoResult.UserNotFound>(result)
        verify(exactly = 0) { imageStorage.upload(any(), any(), any(), any()) }
    }

    @Test
    fun `should upload the new photo, persist its key and return a signed url`() {
        every { userRepository.findById(userId) } returns user
        val keySlot = slot<String>()
        every { imageStorage.signedUrl(capture(keySlot), any()) } returns "https://signed.example/photo"

        val result = sut.updatePhoto(UpdateProfilePhotoCommand(userId, byteArrayOf(1, 2, 3), "image/jpeg"))

        val success = assertIs<UpdateProfilePhotoResult.Success>(result)
        assertEquals("https://signed.example/photo", success.photoUrl)
        assertTrue(keySlot.captured.startsWith("profiles/${userId.value}/"))
        assertTrue(keySlot.captured.endsWith(".jpg"))
        verify { imageStorage.upload(ImageVisibility.PRIVATE, keySlot.captured, any(), "image/jpeg") }
        verify { userRepository.updatePhotoKey(userId, keySlot.captured) }
    }

    @Test
    fun `should delete the previous photo when replacing it`() {
        every { userRepository.findById(userId) } returns user.copy(photoKey = "profiles/old.jpg")

        sut.updatePhoto(UpdateProfilePhotoCommand(userId, byteArrayOf(1, 2, 3), "image/webp"))

        verify { imageStorage.delete(ImageVisibility.PRIVATE, "profiles/old.jpg") }
    }
}

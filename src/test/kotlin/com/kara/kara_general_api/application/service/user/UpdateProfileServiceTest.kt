package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileResult
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class UpdateProfileServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>(relaxed = true)
    private val emailVerificationCodeRepository = mockk<EmailVerificationCodeRepository>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        UpdateProfileService(userRepository, firebaseAuthGateway, emailVerificationCodeRepository, emailService)

    private val userId = UserId(UUID.randomUUID())
    private val existingUser =
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
            emailVerified = true,
        )

    @Test
    fun `should update provided fields and keep the others`() {
        every { userRepository.findById(userId) } returns existingUser
        val saved = slot<User>()
        every { userRepository.update(capture(saved)) } answers { saved.captured }

        val command =
            UpdateProfileCommand(
                userId = userId,
                firstName = "Janet",
                lastName = null,
                phoneNumber = null,
                birthDate = null,
                email = null,
            )

        val result = sut.updateProfile(command)

        val success = assertIs<UpdateProfileResult.Success>(result)
        assertEquals("Janet", success.user.firstName)
        assertEquals("Doe", success.user.lastName)
        assertEquals(PhoneNumber("+33612345678"), success.user.phoneNumber)
        verify { firebaseAuthGateway wasNot Called }
        verify { emailService wasNot Called }
    }

    @Test
    fun `should return UserNotFound when the user does not exist`() {
        every { userRepository.findById(userId) } returns null

        val command =
            UpdateProfileCommand(userId, "Janet", null, null, null, null)

        assertEquals(UpdateProfileResult.UserNotFound, sut.updateProfile(command))
    }

    @Test
    fun `should resync firebase, reset verification and send a code when email changes`() {
        val newEmail = Email("janet@example.com")
        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.existsByEmail(newEmail) } returns false
        val saved = slot<User>()
        every { userRepository.update(capture(saved)) } answers { saved.captured }

        val command =
            UpdateProfileCommand(userId, null, null, null, null, newEmail)

        val result = sut.updateProfile(command)

        val success = assertIs<UpdateProfileResult.Success>(result)
        assertEquals(newEmail, success.user.email)
        assertFalse(success.user.emailVerified)
        verify { firebaseAuthGateway.updateEmail(FirebaseUserId("firebase-uid"), newEmail) }
        verify { emailVerificationCodeRepository.save(newEmail, any(), Duration.ofMinutes(10)) }
        verify { emailService.sendVerificationCode(newEmail, any()) }
    }

    @Test
    fun `should return EmailAlreadyUsed and touch nothing when the new email is taken`() {
        val newEmail = Email("taken@example.com")
        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.existsByEmail(newEmail) } returns true

        val command =
            UpdateProfileCommand(userId, null, null, null, null, newEmail)

        val result = sut.updateProfile(command)

        assertEquals(UpdateProfileResult.EmailAlreadyUsed, result)
        verify { firebaseAuthGateway wasNot Called }
        verify(exactly = 0) { userRepository.update(any()) }
        verify { emailService wasNot Called }
    }

    @Test
    fun `should not run the email flow when the email is unchanged`() {
        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.update(any()) } answers { firstArg() }

        val command =
            UpdateProfileCommand(userId, "Janet", null, null, null, existingUser.email)

        val result = sut.updateProfile(command)

        assertIs<UpdateProfileResult.Success>(result)
        verify { firebaseAuthGateway wasNot Called }
        verify { emailService wasNot Called }
    }
}

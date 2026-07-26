package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordResult
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.PasswordResetCodeRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResetPasswordServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordResetCodeRepository = mockk<PasswordResetCodeRepository>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>(relaxed = true)
    private val sut =
        ResetPasswordService(userRepository, passwordResetCodeRepository, passwordHasher, firebaseAuthGateway)

    private val email = Email("client@kara.app")
    private val user =
        User(
            id = UserId(UUID.randomUUID()),
            email = email,
            hashedPassword = HashedPassword("old-hash"),
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1995, 5, 20),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = true,
        )
    private val validCommand = ResetPasswordCommand(email = email, code = "123456", newPassword = "Azerty123")

    @Test
    fun `should return UserNotFound when email is unknown`() {
        every { userRepository.findByEmail(email) } returns null

        val result = sut.resetPassword(validCommand)

        assertEquals(ResetPasswordResult.UserNotFound, result)
    }

    @Test
    fun `should return CodeExpiredOrMissing when no code in Redis`() {
        every { userRepository.findByEmail(email) } returns user
        every { passwordResetCodeRepository.find(email) } returns null

        val result = sut.resetPassword(validCommand)

        assertEquals(ResetPasswordResult.CodeExpiredOrMissing, result)
    }

    @Test
    fun `should return InvalidCode when code does not match`() {
        every { userRepository.findByEmail(email) } returns user
        every { passwordResetCodeRepository.find(email) } returns "999999"

        val result = sut.resetPassword(validCommand)

        assertEquals(ResetPasswordResult.InvalidCode, result)
    }

    @Test
    fun `should return InvalidPassword when new password is too weak`() {
        every { userRepository.findByEmail(email) } returns user
        every { passwordResetCodeRepository.find(email) } returns "123456"

        val result = sut.resetPassword(validCommand.copy(newPassword = "abc"))

        assertIs<ResetPasswordResult.InvalidPassword>(result)
    }

    @Test
    fun `should update password in DB and Firebase on success`() {
        every { userRepository.findByEmail(email) } returns user
        every { passwordResetCodeRepository.find(email) } returns "123456"
        every { passwordHasher.hash(validCommand.newPassword) } returns HashedPassword("new-hash")
        justRun { userRepository.updatePassword(user.id, HashedPassword("new-hash")) }

        val result = sut.resetPassword(validCommand)

        assertEquals(ResetPasswordResult.Success, result)
        verify { userRepository.updatePassword(user.id, HashedPassword("new-hash")) }
        verify { firebaseAuthGateway.updatePassword(FirebaseUserId(user.firebaseUid), validCommand.newPassword) }
        verify { passwordResetCodeRepository.delete(email) }
    }

    @Test
    fun `should not delete code if Firebase update throws`() {
        every { userRepository.findByEmail(email) } returns user
        every { passwordResetCodeRepository.find(email) } returns "123456"
        every { passwordHasher.hash(validCommand.newPassword) } returns HashedPassword("new-hash")
        justRun { userRepository.updatePassword(any(), any()) }
        every { firebaseAuthGateway.updatePassword(any(), any()) } throws RuntimeException("Firebase down")

        runCatching { sut.resetPassword(validCommand) }

        verify(exactly = 0) { passwordResetCodeRepository.delete(email) }
    }
}

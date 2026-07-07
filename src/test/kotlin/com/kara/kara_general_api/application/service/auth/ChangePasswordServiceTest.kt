package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordResult
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val STAFF_PASSWORD = "Str0ng!P@sswordStr0ng!P@sswordXY"

class ChangePasswordServiceTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>(relaxed = true)
    private val sut = ChangePasswordService(userRepository, passwordHasher, firebaseAuthGateway)

    private val userId = UserId(UUID.randomUUID())

    private fun serverUser(): User =
        User(
            id = userId,
            email = Email("server@kara.app"),
            hashedPassword = HashedPassword("current-hash"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = UserRole.SERVER,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            mustChangePassword = true,
        )

    @Test
    fun `should change the password when current password matches and new one is strong`() {
        every { userRepository.findById(userId) } returns serverUser()
        every { passwordHasher.matches("current", HashedPassword("current-hash")) } returns true
        every { passwordHasher.hash(STAFF_PASSWORD) } returns HashedPassword("new-hash")

        val result = sut.changePassword(ChangePasswordCommand(userId, "current", STAFF_PASSWORD))

        assertEquals(ChangePasswordResult.Success, result)
        verify { firebaseAuthGateway.updatePassword(FirebaseUserId("firebase-uid"), STAFF_PASSWORD) }
        verify { userRepository.updatePassword(userId, HashedPassword("new-hash")) }
    }

    @Test
    fun `should return UserNotFound when the account does not exist`() {
        every { userRepository.findById(userId) } returns null

        val result = sut.changePassword(ChangePasswordCommand(userId, "current", STAFF_PASSWORD))

        assertEquals(ChangePasswordResult.UserNotFound, result)
    }

    @Test
    fun `should return InvalidCurrentPassword when the current password does not match`() {
        every { userRepository.findById(userId) } returns serverUser()
        every { passwordHasher.matches("wrong", HashedPassword("current-hash")) } returns false

        val result = sut.changePassword(ChangePasswordCommand(userId, "wrong", STAFF_PASSWORD))

        assertEquals(ChangePasswordResult.InvalidCurrentPassword, result)
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    fun `should return WeakPassword when the new password violates the staff policy`() {
        every { userRepository.findById(userId) } returns serverUser()
        every { passwordHasher.matches("current", HashedPassword("current-hash")) } returns true

        val result = sut.changePassword(ChangePasswordCommand(userId, "current", "short"))

        assertIs<ChangePasswordResult.WeakPassword>(result)
        verify(exactly = 0) { firebaseAuthGateway.updatePassword(any(), any()) }
    }
}

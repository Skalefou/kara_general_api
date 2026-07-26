package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountResult
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
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
import kotlin.test.assertNotNull

class DeactivateAccountServiceTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
    private val sut = DeactivateAccountService(userRepository, refreshTokenRepository)

    private val userId = UserId(UUID.randomUUID())

    private fun account(deactivatedAt: Instant? = null): User =
        User(
            id = userId,
            email = Email("server@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = UserRole.SERVER,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            deactivatedAt = deactivatedAt,
        )

    @Test
    fun `should deactivate an active account`() {
        every { userRepository.findById(userId) } returns account()
        val saved = slot<User>()
        every { userRepository.update(capture(saved)) } answers { saved.captured }

        val result = sut.deactivate(DeactivateAccountCommand(userId))

        assertEquals(DeactivateAccountResult.Success, result)
        assertNotNull(saved.captured.deactivatedAt)
        verify { refreshTokenRepository.revokeAllForUser(userId) }
    }

    @Test
    fun `should return UserNotFound when the account does not exist`() {
        every { userRepository.findById(userId) } returns null

        val result = sut.deactivate(DeactivateAccountCommand(userId))

        assertEquals(DeactivateAccountResult.UserNotFound, result)
        verify(exactly = 0) { userRepository.update(any()) }
        verify(exactly = 0) { refreshTokenRepository.revokeAllForUser(any()) }
    }

    @Test
    fun `should return AlreadyDeactivated when the account is already deactivated`() {
        every { userRepository.findById(userId) } returns account(deactivatedAt = Instant.now())

        val result = sut.deactivate(DeactivateAccountCommand(userId))

        assertEquals(DeactivateAccountResult.AlreadyDeactivated, result)
        verify(exactly = 0) { userRepository.update(any()) }
        verify(exactly = 0) { refreshTokenRepository.revokeAllForUser(any()) }
    }
}

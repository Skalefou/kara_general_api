package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenCommand
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenResult
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
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

class RefreshTokenServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val tokenService = mockk<TokenService>()
    private val sut = RefreshTokenService(userRepository, refreshTokenRepository, tokenService)

    private val command = RefreshTokenCommand(refreshToken = "old-refresh-token")

    private val user =
        User(
            id = UserId(UUID.randomUUID()),
            email = Email("client@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1995, 5, 20),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = true,
        )

    @Test
    fun `should return new access token and refresh token when token is valid`() {
        every { refreshTokenRepository.redeem(command.refreshToken) } returns user.id.value
        every { userRepository.findById(user.id) } returns user
        every { tokenService.generateAccessToken(user) } returns AccessToken("new-jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("new-refresh-token", 604800)

        val result = sut.refresh(command)

        assertIs<RefreshTokenResult.Success>(result)
        assertEquals("new-jwt-token", result.accessToken.value)
        assertEquals("new-refresh-token", result.refreshToken.value)
    }

    @Test
    fun `should return InvalidToken when token is unknown or expired`() {
        every { refreshTokenRepository.redeem(command.refreshToken) } returns null

        val result = sut.refresh(command)

        assertEquals(RefreshTokenResult.InvalidToken, result)
    }

    @Test
    fun `should return InvalidToken without issuing a new token when the owning user no longer exists`() {
        every { refreshTokenRepository.redeem(command.refreshToken) } returns user.id.value
        every { userRepository.findById(user.id) } returns null

        val result = sut.refresh(command)

        assertEquals(RefreshTokenResult.InvalidToken, result)
        verify(exactly = 0) { refreshTokenRepository.issue(any()) }
    }

    @Test
    fun `should return InvalidToken without issuing a new token when the account is deactivated`() {
        every { refreshTokenRepository.redeem(command.refreshToken) } returns user.id.value
        every { userRepository.findById(user.id) } returns user.copy(deactivatedAt = Instant.now())

        val result = sut.refresh(command)

        assertEquals(RefreshTokenResult.InvalidToken, result)
        verify(exactly = 0) { refreshTokenRepository.issue(any()) }
    }
}

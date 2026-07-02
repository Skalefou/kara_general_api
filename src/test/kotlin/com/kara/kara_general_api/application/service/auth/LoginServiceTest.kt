package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.LoginCommand
import com.kara.kara_general_api.domain.port.input.auth.LoginIdentifier
import com.kara.kara_general_api.domain.port.input.auth.LoginResult
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoginServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val tokenService = mockk<TokenService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val sut = LoginService(userRepository, passwordHasher, tokenService, refreshTokenRepository)

    private val email = Email("client@kara.app")
    private val phoneNumber = PhoneNumber("0612345678")

    private val user =
        User(
            id = UserId(UUID.randomUUID()),
            email = email,
            hashedPassword = HashedPassword("hashed"),
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = phoneNumber,
            birthDate = LocalDate.of(1995, 5, 20),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = true,
        )

    @Test
    fun `should return access token and user when credentials match using email`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { tokenService.generateAccessToken(user) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604800)

        val result = sut.login(command)

        assertIs<LoginResult.Success>(result)
        assertEquals("jwt-token", result.accessToken.value)
        assertEquals("refresh-token-value", result.refreshToken.value)
        assertEquals(user, result.user)
    }

    @Test
    fun `should return access token and user when credentials match using phone number`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByPhoneNumber(phoneNumber), password = "S3cur3P@ssw0rd")
        every { userRepository.findByPhoneNumber(phoneNumber) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { tokenService.generateAccessToken(user) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604800)

        val result = sut.login(command)

        assertIs<LoginResult.Success>(result)
        assertEquals("jwt-token", result.accessToken.value)
    }

    @Test
    fun `should return UserNotFound when no account matches the identifier`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns null

        val result = sut.login(command)

        assertEquals(LoginResult.UserNotFound, result)
    }

    @Test
    fun `should return InvalidCredentials when password does not match`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "wrong-password")
        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns false

        val result = sut.login(command)

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun `should return AccountDeleted when the account has been anonymized`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns user.copy(deletedAt = Instant.now())

        val result = sut.login(command)

        assertEquals(LoginResult.AccountDeleted, result)
    }
}

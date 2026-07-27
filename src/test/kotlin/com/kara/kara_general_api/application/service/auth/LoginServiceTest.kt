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
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorStatus
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.MfaChallengeRepository
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val tokenService = mockk<TokenService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val twoFactorRepository = mockk<TwoFactorRepository>()
    private val mfaChallengeRepository = mockk<MfaChallengeRepository>()
    private val sut =
        LoginService(
            userRepository,
            passwordHasher,
            tokenService,
            refreshTokenRepository,
            twoFactorRepository,
            mfaChallengeRepository,
        )

    init {
        // Par défaut, aucun compte n'a d'A2F : les tests de non-régression du chemin sans A2F valent tels quels.
        every { twoFactorRepository.findByUserId(any()) } returns null
    }

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

    @Test
    fun `should return AccountDeactivated when the account has been deactivated`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns user.copy(deactivatedAt = Instant.now())

        val result = sut.login(command)

        assertEquals(LoginResult.AccountDeactivated, result)
    }

    @Test
    fun `should return TempPasswordExpired when the temporary password has expired`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns
            user.copy(mustChangePassword = true, tempPasswordExpiresAt = Instant.now().minusSeconds(1))
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true

        val result = sut.login(command)

        assertEquals(LoginResult.TempPasswordExpired, result)
    }

    @Test
    fun `should flag mustChangePassword when the temporary password is still valid`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns
            user.copy(mustChangePassword = true, tempPasswordExpiresAt = Instant.now().plusSeconds(3600))
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { tokenService.generateAccessToken(any()) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604800)

        val result = sut.login(command)

        assertIs<LoginResult.Success>(result)
        assertTrue(result.mustChangePassword)
    }

    @Test
    fun `should return TwoFactorRequired and no token when the account has active two-factor`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns activeTwoFactorSecret
        every { mfaChallengeRepository.issue(user.id, Duration.ofMinutes(5)) } returns "mfa-token"

        val result = sut.login(command)

        assertIs<LoginResult.TwoFactorRequired>(result)
        assertEquals("mfa-token", result.mfaToken)
        assertEquals(300, result.expiresInSeconds)
        verify(exactly = 0) { tokenService.generateAccessToken(any()) }
        verify(exactly = 0) { refreshTokenRepository.issue(any()) }
    }

    @Test
    fun `should ignore a pending two-factor setup that was never confirmed`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "S3cur3P@ssw0rd")
        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns
            activeTwoFactorSecret.copy(status = TwoFactorStatus.PENDING, activatedAt = null)
        every { tokenService.generateAccessToken(user) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604800)

        val result = sut.login(command)

        assertIs<LoginResult.Success>(result)
    }

    @Test
    fun `should not issue a two-factor challenge when the password is wrong`() {
        val command = LoginCommand(identifier = LoginIdentifier.ByEmail(email), password = "wrong-password")
        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns false

        assertEquals(LoginResult.InvalidCredentials, sut.login(command))
        verify(exactly = 0) { mfaChallengeRepository.issue(any(), any()) }
    }

    private val activeTwoFactorSecret =
        TwoFactorSecret(
            userId = user.id,
            secretCipher = "cipher",
            status = TwoFactorStatus.ACTIVE,
            createdAt = Instant.now(),
            activatedAt = Instant.now(),
        )
}

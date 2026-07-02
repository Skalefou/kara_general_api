package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailCommand
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailResult
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
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

class VerifyEmailServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val emailVerificationCodeRepository = mockk<EmailVerificationCodeRepository>()
    private val tokenService = mockk<TokenService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val sut = VerifyEmailService(userRepository, emailVerificationCodeRepository, tokenService, refreshTokenRepository)

    private val command = VerifyEmailCommand(email = Email("client@kara.app"), code = "123456")

    private val user =
        User(
            id = UserId(UUID.randomUUID()),
            email = command.email,
            hashedPassword = HashedPassword("hashed"),
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1995, 5, 20),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = false,
        )

    @Test
    fun `should return access token and mark email verified when code matches`() {
        every { userRepository.findByEmail(command.email) } returns user
        every { emailVerificationCodeRepository.find(command.email) } returns "123456"
        every { userRepository.markEmailVerified(user.id) } returns Unit
        every { emailVerificationCodeRepository.delete(command.email) } returns Unit
        every { tokenService.generateAccessToken(any()) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604800)

        val result = sut.verify(command)

        assertIs<VerifyEmailResult.Success>(result)
        assertEquals("jwt-token", result.accessToken.value)
        assertEquals("refresh-token-value", result.refreshToken.value)
        verify { userRepository.markEmailVerified(user.id) }
        verify { emailVerificationCodeRepository.delete(command.email) }
    }

    @Test
    fun `should return UserNotFound when no account matches the email`() {
        every { userRepository.findByEmail(command.email) } returns null

        val result = sut.verify(command)

        assertEquals(VerifyEmailResult.UserNotFound, result)
    }

    @Test
    fun `should return AlreadyVerified when email is already verified`() {
        every { userRepository.findByEmail(command.email) } returns user.copy(emailVerified = true)

        val result = sut.verify(command)

        assertEquals(VerifyEmailResult.AlreadyVerified, result)
    }

    @Test
    fun `should return CodeExpiredOrMissing when no code is stored`() {
        every { userRepository.findByEmail(command.email) } returns user
        every { emailVerificationCodeRepository.find(command.email) } returns null

        val result = sut.verify(command)

        assertEquals(VerifyEmailResult.CodeExpiredOrMissing, result)
    }

    @Test
    fun `should return InvalidCode when stored code does not match`() {
        every { userRepository.findByEmail(command.email) } returns user
        every { emailVerificationCodeRepository.find(command.email) } returns "000000"

        val result = sut.verify(command)

        assertEquals(VerifyEmailResult.InvalidCode, result)
    }
}

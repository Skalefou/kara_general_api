package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordCommand
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordResult
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordResetCodeRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class ForgotPasswordServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordResetCodeRepository = mockk<PasswordResetCodeRepository>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut = ForgotPasswordService(userRepository, passwordResetCodeRepository, emailService)

    private val email = Email("client@kara.app")
    private val user =
        User(
            id = UserId(UUID.randomUUID()),
            email = email,
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
    fun `should send reset code when user exists`() {
        every { userRepository.findByEmail(email) } returns user

        val result = sut.requestReset(ForgotPasswordCommand(email))

        assertEquals(ForgotPasswordResult.Success, result)
        verify { passwordResetCodeRepository.save(email, any(), any()) }
        verify { emailService.sendPasswordResetCode(email, any()) }
    }

    @Test
    fun `should return Success silently when email is unknown`() {
        every { userRepository.findByEmail(email) } returns null

        val result = sut.requestReset(ForgotPasswordCommand(email))

        assertEquals(ForgotPasswordResult.Success, result)
        verify(exactly = 0) { passwordResetCodeRepository.save(email, any(), any()) }
        verify(exactly = 0) { emailService.sendPasswordResetCode(email, any()) }
    }
}

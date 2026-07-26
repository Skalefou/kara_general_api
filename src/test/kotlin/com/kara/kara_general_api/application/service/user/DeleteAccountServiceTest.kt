package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountCommand
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
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

class DeleteAccountServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>(relaxed = true)
    private val emailVerificationCodeRepository = mockk<EmailVerificationCodeRepository>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        DeleteAccountService(
            userRepository,
            passwordHasher,
            firebaseAuthGateway,
            emailVerificationCodeRepository,
            emailService,
        )

    private val userId = UserId(UUID.randomUUID())
    private val user =
        User(
            id = userId,
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
    private val command = DeleteAccountCommand(userId = userId, password = "Azerty123")

    @Test
    fun `should return UserNotFound when user does not exist`() {
        every { userRepository.findById(userId) } returns null

        val result = sut.deleteAccount(command)

        assertEquals(DeleteAccountResult.UserNotFound, result)
        verify(exactly = 0) { userRepository.anonymize(any()) }
    }

    @Test
    fun `should return InvalidPassword when password is wrong`() {
        every { userRepository.findById(userId) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns false

        val result = sut.deleteAccount(command)

        assertEquals(DeleteAccountResult.InvalidPassword, result)
        verify(exactly = 0) { userRepository.anonymize(any()) }
        verify(exactly = 0) { firebaseAuthGateway.deleteUser(any()) }
    }

    @Test
    fun `should anonymize user and call all ports on success`() {
        every { userRepository.findById(userId) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        justRun { userRepository.anonymize(userId) }

        val result = sut.deleteAccount(command)

        assertEquals(DeleteAccountResult.Success, result)
        verify { emailService.sendAccountDeletionConfirmation(user.email) }
        verify { firebaseAuthGateway.deleteUser(FirebaseUserId(user.firebaseUid)) }
        verify { emailVerificationCodeRepository.delete(user.email) }
        verify { userRepository.anonymize(userId) }
    }

    @Test
    fun `should not anonymize if Firebase deletion throws`() {
        every { userRepository.findById(userId) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { firebaseAuthGateway.deleteUser(any()) } throws RuntimeException("Firebase down")

        runCatching { sut.deleteAccount(command) }

        verify(exactly = 0) { userRepository.anonymize(any()) }
    }
}

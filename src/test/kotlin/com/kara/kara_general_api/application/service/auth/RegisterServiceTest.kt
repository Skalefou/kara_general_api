package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.RegisterCommand
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.output.EmailAlreadyUsedException
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RegisterServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val emailVerificationCodeRepository = mockk<EmailVerificationCodeRepository>(relaxed = true)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        RegisterService(
            userRepository,
            firebaseAuthGateway,
            passwordHasher,
            emailVerificationCodeRepository,
            emailService,
        )

    private val command =
        RegisterCommand(
            email = Email("client@kara.app"),
            plainPassword = "Azerty123",
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1995, 5, 20),
        )

    @Test
    fun `should create firebase account and persist user when registration succeeds`() {
        every { userRepository.existsByEmail(command.email) } returns false
        every { firebaseAuthGateway.createUser(command.email, command.plainPassword) } returns FirebaseUserId("firebase-uid")
        every { passwordHasher.hash(command.plainPassword) } returns HashedPassword("hashed")
        every { userRepository.save(any()) } answers { firstArg<User>() }

        val result = sut.register(command)

        assertIs<RegisterResult.Success>(result)
        assertEquals("firebase-uid", result.user.firebaseUid)
        verify { userRepository.save(any()) }
        verify { emailVerificationCodeRepository.save(command.email, any(), any()) }
        verify { emailService.sendVerificationCode(command.email, any()) }
    }

    @Test
    fun `should return EmailAlreadyUsed when email is already registered`() {
        every { userRepository.existsByEmail(command.email) } returns true

        val result = sut.register(command)

        assertEquals(RegisterResult.EmailAlreadyUsed, result)
        verify(exactly = 0) { firebaseAuthGateway.createUser(command.email, command.plainPassword) }
    }

    @Test
    fun `should return EmailAlreadyUsed when firebase reports email already exists`() {
        every { userRepository.existsByEmail(command.email) } returns false
        every {
            firebaseAuthGateway.createUser(command.email, command.plainPassword)
        } throws EmailAlreadyUsedException(command.email)

        val result = sut.register(command)

        assertEquals(RegisterResult.EmailAlreadyUsed, result)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should return InvalidPassword when password is too weak`() {
        every { userRepository.existsByEmail(command.email) } returns false
        val weakCommand = command.copy(plainPassword = "abc")

        val result = sut.register(weakCommand)

        assertIs<RegisterResult.InvalidPassword>(result)
        assertTrue(result.reasons.isNotEmpty())
        verify(exactly = 0) { firebaseAuthGateway.createUser(weakCommand.email, weakCommand.plainPassword) }
    }

    @Test
    fun `should rollback firebase account when persistence fails`() {
        every { userRepository.existsByEmail(command.email) } returns false
        every { firebaseAuthGateway.createUser(command.email, command.plainPassword) } returns FirebaseUserId("firebase-uid")
        every { passwordHasher.hash(command.plainPassword) } returns HashedPassword("hashed")
        every { userRepository.save(any()) } throws RuntimeException("db down")
        every { firebaseAuthGateway.deleteUser(FirebaseUserId("firebase-uid")) } returns Unit

        runCatching { sut.register(command) }

        verify { firebaseAuthGateway.deleteUser(FirebaseUserId("firebase-uid")) }
    }
}

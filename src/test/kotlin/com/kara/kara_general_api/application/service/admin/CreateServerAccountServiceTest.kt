package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountResult
import com.kara.kara_general_api.domain.port.output.EmailAlreadyUsedException
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import com.kara.kara_general_api.domain.port.output.PasswordGenerator
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

private const val STAFF_PASSWORD = "Str0ng!P@sswordStr0ng!P@sswordXY"

class CreateServerAccountServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val passwordGenerator = mockk<PasswordGenerator>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        CreateServerAccountService(
            userRepository,
            firebaseAuthGateway,
            passwordHasher,
            passwordGenerator,
            emailService,
        )

    private val command =
        CreateServerAccountCommand(
            email = Email("server@kara.app"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
        )

    @Test
    fun `should create a server account and send an invitation email`() {
        every { userRepository.existsByEmail(command.email) } returns false
        every { passwordGenerator.generate(UserRole.SERVER) } returns STAFF_PASSWORD
        every { firebaseAuthGateway.createUser(command.email, STAFF_PASSWORD) } returns FirebaseUserId("firebase-uid")
        every { passwordHasher.hash(STAFF_PASSWORD) } returns HashedPassword("hashed")
        every { userRepository.save(any()) } answers { firstArg<User>() }

        val result = sut.createServerAccount(command)

        assertIs<CreateServerAccountResult.Success>(result)
        assertEquals(UserRole.SERVER, result.user.role)
        assertTrue(result.user.mustChangePassword)
        verify { emailService.sendServerInvitation(command.email, "Jane", STAFF_PASSWORD, any()) }
    }

    @Test
    fun `should return EmailAlreadyUsed when the email already exists`() {
        every { userRepository.existsByEmail(command.email) } returns true

        val result = sut.createServerAccount(command)

        assertEquals(CreateServerAccountResult.EmailAlreadyUsed, result)
        verify(exactly = 0) { firebaseAuthGateway.createUser(command.email, any()) }
    }

    @Test
    fun `should return EmailAlreadyUsed when firebase reports the email exists`() {
        every { userRepository.existsByEmail(command.email) } returns false
        every { passwordGenerator.generate(UserRole.SERVER) } returns STAFF_PASSWORD
        every {
            firebaseAuthGateway.createUser(command.email, STAFF_PASSWORD)
        } throws EmailAlreadyUsedException(command.email)

        val result = sut.createServerAccount(command)

        assertEquals(CreateServerAccountResult.EmailAlreadyUsed, result)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should rollback firebase account when persistence fails`() {
        every { userRepository.existsByEmail(command.email) } returns false
        every { passwordGenerator.generate(UserRole.SERVER) } returns STAFF_PASSWORD
        every { firebaseAuthGateway.createUser(command.email, STAFF_PASSWORD) } returns FirebaseUserId("firebase-uid")
        every { passwordHasher.hash(STAFF_PASSWORD) } returns HashedPassword("hashed")
        every { userRepository.save(any()) } throws RuntimeException("db down")
        every { firebaseAuthGateway.deleteUser(FirebaseUserId("firebase-uid")) } returns Unit

        runCatching { sut.createServerAccount(command) }

        verify { firebaseAuthGateway.deleteUser(FirebaseUserId("firebase-uid")) }
    }
}

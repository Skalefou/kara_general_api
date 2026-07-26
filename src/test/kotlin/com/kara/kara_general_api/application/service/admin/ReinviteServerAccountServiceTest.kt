package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountResult
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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

private const val STAFF_PASSWORD = "Str0ng!P@sswordStr0ng!P@sswordXY"

class ReinviteServerAccountServiceTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val firebaseAuthGateway = mockk<FirebaseAuthGateway>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private val passwordGenerator = mockk<PasswordGenerator>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        ReinviteServerAccountService(
            userRepository,
            firebaseAuthGateway,
            passwordHasher,
            passwordGenerator,
            emailService,
        )

    private val serverId = UserId(UUID.randomUUID())

    private fun server(role: UserRole = UserRole.SERVER): User =
        User(
            id = serverId,
            email = Email("server@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = role,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
        )

    @Test
    fun `should generate a new temporary password and send an invitation`() {
        every { userRepository.findById(serverId) } returns server()
        every { passwordGenerator.generate(UserRole.SERVER) } returns STAFF_PASSWORD
        every { passwordHasher.hash(STAFF_PASSWORD) } returns HashedPassword("new-hash")

        val result = sut.reinvite(ReinviteServerAccountCommand(serverId))

        assertEquals(ReinviteServerAccountResult.Success, result)
        verify { firebaseAuthGateway.updatePassword(FirebaseUserId("firebase-uid"), STAFF_PASSWORD) }
        verify { userRepository.applyReinvitation(serverId, HashedPassword("new-hash"), any()) }
        verify { emailService.sendServerInvitation(Email("server@kara.app"), "Jane", STAFF_PASSWORD, any()) }
    }

    @Test
    fun `should return UserNotFound when the account does not exist`() {
        every { userRepository.findById(serverId) } returns null

        val result = sut.reinvite(ReinviteServerAccountCommand(serverId))

        assertEquals(ReinviteServerAccountResult.UserNotFound, result)
        verify(exactly = 0) { userRepository.applyReinvitation(any(), any(), any()) }
    }

    @Test
    fun `should return NotAServer when the account is not a server`() {
        every { userRepository.findById(serverId) } returns server(role = UserRole.CLIENT)

        val result = sut.reinvite(ReinviteServerAccountCommand(serverId))

        assertEquals(ReinviteServerAccountResult.NotAServer, result)
        verify(exactly = 0) { firebaseAuthGateway.updatePassword(any(), any()) }
    }
}

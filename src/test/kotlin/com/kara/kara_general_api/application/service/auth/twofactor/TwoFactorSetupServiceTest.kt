package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorStatus
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorResult
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TwoFactorSetupServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val twoFactorRepository = mockk<TwoFactorRepository>(relaxed = true)
    private val totpService = mockk<TotpService>()
    private val secretCipher = mockk<SecretCipher>()
    private val sut =
        TwoFactorSetupService(userRepository, passwordHasher, twoFactorRepository, totpService, secretCipher)

    private val user = TwoFactorTestFixtures.user
    private val command = SetupTwoFactorCommand(userId = user.id, password = "S3cur3P@ssw0rd")

    @Test
    fun `should return the plain secret and otpauth uri when setup succeeds`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns null
        every { totpService.generateSecret() } returns "PLAINSECRET"
        every { secretCipher.encrypt("PLAINSECRET") } returns "encrypted"
        every { totpService.otpauthUri("PLAINSECRET", user.email.value) } returns "otpauth://totp/Kara:client@kara.app"

        val result = sut.setup(command)

        assertIs<SetupTwoFactorResult.Success>(result)
        assertEquals("PLAINSECRET", result.secret)
        assertEquals("otpauth://totp/Kara:client@kara.app", result.otpauthUri)
    }

    @Test
    fun `should persist the secret encrypted and pending when setup succeeds`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns null
        every { totpService.generateSecret() } returns "PLAINSECRET"
        every { secretCipher.encrypt("PLAINSECRET") } returns "encrypted"
        every { totpService.otpauthUri(any(), any()) } returns "otpauth://totp/Kara:client@kara.app"
        val saved = slot<TwoFactorSecret>()
        every { twoFactorRepository.save(capture(saved)) } answers { saved.captured }

        sut.setup(command)

        assertEquals("encrypted", saved.captured.secretCipher)
        assertEquals(TwoFactorStatus.PENDING, saved.captured.status)
    }

    @Test
    fun `should return UserNotFound when no account matches the identifier`() {
        every { userRepository.findById(user.id) } returns null

        assertEquals(SetupTwoFactorResult.UserNotFound, sut.setup(command))
    }

    @Test
    fun `should return InvalidPassword when the current password does not match`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns false

        assertEquals(SetupTwoFactorResult.InvalidPassword, sut.setup(command))
        verify(exactly = 0) { twoFactorRepository.save(any()) }
    }

    @Test
    fun `should return AlreadyEnabled when two-factor is already active`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()

        assertEquals(SetupTwoFactorResult.AlreadyEnabled, sut.setup(command))
        verify(exactly = 0) { twoFactorRepository.save(any()) }
    }

    @Test
    fun `should overwrite a previous pending setup instead of rejecting it`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.pendingSecret("old")
        every { totpService.generateSecret() } returns "NEWSECRET"
        every { secretCipher.encrypt("NEWSECRET") } returns "new-encrypted"
        every { totpService.otpauthUri(any(), any()) } returns "otpauth://totp/Kara:client@kara.app"

        val result = sut.setup(command)

        assertIs<SetupTwoFactorResult.Success>(result)
        verify { twoFactorRepository.save(match { it.secretCipher == "new-encrypted" }) }
    }
}

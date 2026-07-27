package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorResult
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TwoFactorDisableServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val twoFactorRepository = mockk<TwoFactorRepository>(relaxed = true)
    private val recoveryCodeRepository = mockk<RecoveryCodeRepository>(relaxed = true)
    private val totpService = mockk<TotpService>()
    private val secretCipher = mockk<SecretCipher>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        TwoFactorDisableService(
            userRepository,
            passwordHasher,
            twoFactorRepository,
            recoveryCodeRepository,
            totpService,
            secretCipher,
            emailService,
        )

    private val user = TwoFactorTestFixtures.user
    private val command =
        DisableTwoFactorCommand(userId = user.id, password = "S3cur3P@ssw0rd", code = "123456")

    @Test
    fun `should wipe the secret and every recovery code when password and totp code are valid`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", command.code) } returns true

        assertEquals(DisableTwoFactorResult.Success, sut.disable(command))
        verify { twoFactorRepository.deleteByUserId(user.id) }
        verify { recoveryCodeRepository.deleteByUserId(user.id) }
        verify { emailService.sendTwoFactorDisabled(user.email) }
    }

    @Test
    fun `should accept an unused recovery code as second factor when the totp code is not valid`() {
        val recoveryCommand = command.copy(code = "cascade tulipe marteau renard")
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(recoveryCommand.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", recoveryCommand.code) } returns false
        every { recoveryCodeRepository.findUnusedByUserId(user.id) } returns
            listOf(TwoFactorTestFixtures.recoveryCode("recovery-hash"))
        every {
            passwordHasher.matches("cascade tulipe marteau renard", HashedPassword("recovery-hash"))
        } returns true

        assertEquals(DisableTwoFactorResult.Success, sut.disable(recoveryCommand))
        verify { twoFactorRepository.deleteByUserId(user.id) }
        verify { recoveryCodeRepository.deleteByUserId(user.id) }
    }

    @Test
    fun `should normalize the recovery code before comparing it to the stored hashes`() {
        val messyCommand = command.copy(code = "  CASCADE-Tulipé_marteau   renard ")
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(messyCommand.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", messyCommand.code) } returns false
        every { recoveryCodeRepository.findUnusedByUserId(user.id) } returns
            listOf(TwoFactorTestFixtures.recoveryCode("recovery-hash"))
        every {
            passwordHasher.matches("cascade tulipe marteau renard", HashedPassword("recovery-hash"))
        } returns true

        assertEquals(DisableTwoFactorResult.Success, sut.disable(messyCommand))
    }

    @Test
    fun `should return InvalidCode when neither the totp code nor a recovery code matches`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", command.code) } returns false
        every { recoveryCodeRepository.findUnusedByUserId(user.id) } returns emptyList()

        assertEquals(DisableTwoFactorResult.InvalidCode, sut.disable(command))
        verify(exactly = 0) { twoFactorRepository.deleteByUserId(any()) }
        verify(exactly = 0) { emailService.sendTwoFactorDisabled(user.email) }
    }

    @Test
    fun `should return InvalidPassword when the current password does not match`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns false

        assertEquals(DisableTwoFactorResult.InvalidPassword, sut.disable(command))
        verify(exactly = 0) { twoFactorRepository.deleteByUserId(any()) }
    }

    @Test
    fun `should return NotEnabled when two-factor was never activated`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns null

        assertEquals(DisableTwoFactorResult.NotEnabled, sut.disable(command))
    }

    @Test
    fun `should return NotEnabled when only a pending setup exists`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.pendingSecret()

        assertEquals(DisableTwoFactorResult.NotEnabled, sut.disable(command))
    }

    @Test
    fun `should return UserNotFound when no account matches the identifier`() {
        every { userRepository.findById(user.id) } returns null

        assertEquals(DisableTwoFactorResult.UserNotFound, sut.disable(command))
    }
}

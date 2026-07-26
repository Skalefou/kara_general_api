package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorStatus
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorResult
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeGenerator
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
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

class TwoFactorActivationServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val twoFactorRepository = mockk<TwoFactorRepository>(relaxed = true)
    private val recoveryCodeRepository = mockk<RecoveryCodeRepository>(relaxed = true)
    private val recoveryCodeGenerator = mockk<RecoveryCodeGenerator>()
    private val totpService = mockk<TotpService>()
    private val secretCipher = mockk<SecretCipher>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        TwoFactorActivationService(
            userRepository,
            twoFactorRepository,
            recoveryCodeRepository,
            recoveryCodeGenerator,
            totpService,
            secretCipher,
            passwordHasher,
            emailService,
        )

    private val user = TwoFactorTestFixtures.user
    private val command = ActivateTwoFactorCommand(userId = user.id, code = "123456")
    private val generatedCodes = (1..10).map { "code numero $it" }

    private fun givenValidPendingSetup() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.pendingSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", command.code) } returns true
        every { totpService.currentStep() } returns 1_000L
        every { recoveryCodeGenerator.generate(10, 4) } returns generatedCodes
        every { passwordHasher.hash(any()) } answers { HashedPassword("hash:${firstArg<String>()}") }
    }

    @Test
    fun `should return the ten plain recovery codes when the first code is valid`() {
        givenValidPendingSetup()

        val result = sut.activate(command)

        assertIs<ActivateTwoFactorResult.Success>(result)
        assertEquals(generatedCodes, result.recoveryCodes)
    }

    @Test
    fun `should flip the secret to active and consume the current step when activating`() {
        givenValidPendingSetup()

        sut.activate(command)

        verify {
            twoFactorRepository.save(
                match { it.status == TwoFactorStatus.ACTIVE && it.lastUsedStep == 1_000L },
            )
        }
    }

    @Test
    fun `should store only hashed recovery codes when activating`() {
        givenValidPendingSetup()
        val hashes = slot<List<String>>()
        every { recoveryCodeRepository.replaceAll(user.id, capture(hashes)) } returns Unit

        sut.activate(command)

        assertEquals(generatedCodes.map { "hash:$it" }, hashes.captured)
    }

    @Test
    fun `should send the activation email when activating`() {
        givenValidPendingSetup()

        sut.activate(command)

        verify { emailService.sendTwoFactorEnabled(user.email) }
    }

    @Test
    fun `should return InvalidCode when the submitted code does not match the pending secret`() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.pendingSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", command.code) } returns false

        assertEquals(ActivateTwoFactorResult.InvalidCode, sut.activate(command))
        verify(exactly = 0) { twoFactorRepository.save(any()) }
        verify(exactly = 0) { recoveryCodeRepository.replaceAll(any(), any()) }
        verify(exactly = 0) { emailService.sendTwoFactorEnabled(user.email) }
    }

    @Test
    fun `should return SetupNotFound when no secret was ever generated`() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns null

        assertEquals(ActivateTwoFactorResult.SetupNotFound, sut.activate(command))
    }

    @Test
    fun `should return AlreadyEnabled when two-factor is already active`() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()

        assertEquals(ActivateTwoFactorResult.AlreadyEnabled, sut.activate(command))
    }

    @Test
    fun `should return UserNotFound when no account matches the identifier`() {
        every { userRepository.findById(user.id) } returns null

        assertEquals(ActivateTwoFactorResult.UserNotFound, sut.activate(command))
    }
}

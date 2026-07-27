package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesResult
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

class RegenerateRecoveryCodesServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val twoFactorRepository = mockk<TwoFactorRepository>()
    private val recoveryCodeRepository = mockk<RecoveryCodeRepository>(relaxed = true)
    private val recoveryCodeGenerator = mockk<RecoveryCodeGenerator>()
    private val totpService = mockk<TotpService>()
    private val secretCipher = mockk<SecretCipher>()
    private val sut =
        RegenerateRecoveryCodesService(
            userRepository,
            passwordHasher,
            twoFactorRepository,
            recoveryCodeRepository,
            recoveryCodeGenerator,
            totpService,
            secretCipher,
        )

    private val user = TwoFactorTestFixtures.user
    private val command =
        RegenerateRecoveryCodesCommand(userId = user.id, password = "S3cur3P@ssw0rd", code = "123456")
    private val generatedCodes = (1..10).map { "nouveau code $it" }

    private fun givenActiveTwoFactorAndValidCode() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", command.code) } returns true
        every { recoveryCodeGenerator.generate(10, 4) } returns generatedCodes
        every { passwordHasher.hash(any()) } answers { HashedPassword("hash:${firstArg<String>()}") }
    }

    @Test
    fun `should return ten fresh plain codes when password and totp code are valid`() {
        givenActiveTwoFactorAndValidCode()

        val result = sut.regenerate(command)

        assertIs<RegenerateRecoveryCodesResult.Success>(result)
        assertEquals(generatedCodes, result.recoveryCodes)
    }

    @Test
    fun `should replace every previous code by the new hashes when regenerating`() {
        givenActiveTwoFactorAndValidCode()
        val hashes = slot<List<String>>()
        every { recoveryCodeRepository.replaceAll(user.id, capture(hashes)) } returns Unit

        sut.regenerate(command)

        assertEquals(generatedCodes.map { "hash:$it" }, hashes.captured)
    }

    @Test
    fun `should return InvalidCode when the totp code is not valid`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.verify("PLAINSECRET", command.code) } returns false

        assertEquals(RegenerateRecoveryCodesResult.InvalidCode, sut.regenerate(command))
        verify(exactly = 0) { recoveryCodeRepository.replaceAll(any(), any()) }
    }

    @Test
    fun `should return InvalidPassword when the current password does not match`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns false

        assertEquals(RegenerateRecoveryCodesResult.InvalidPassword, sut.regenerate(command))
        verify(exactly = 0) { recoveryCodeRepository.replaceAll(any(), any()) }
    }

    @Test
    fun `should return NotEnabled when two-factor is not active`() {
        every { userRepository.findById(user.id) } returns user
        every { passwordHasher.matches(command.password, user.hashedPassword) } returns true
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.pendingSecret()

        assertEquals(RegenerateRecoveryCodesResult.NotEnabled, sut.regenerate(command))
    }

    @Test
    fun `should return UserNotFound when no account matches the identifier`() {
        every { userRepository.findById(user.id) } returns null

        assertEquals(RegenerateRecoveryCodesResult.UserNotFound, sut.regenerate(command))
    }
}

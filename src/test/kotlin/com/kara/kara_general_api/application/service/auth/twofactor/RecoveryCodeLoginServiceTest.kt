package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ConsumeRecoveryCodeCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ConsumeRecoveryCodeResult
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecoveryCodeLoginServiceTest {
    private val mfaChallengeRepository = FakeMfaChallengeRepository()
    private val userRepository = mockk<UserRepository>()
    private val twoFactorRepository = mockk<TwoFactorRepository>(relaxed = true)
    private val recoveryCodeRepository = mockk<RecoveryCodeRepository>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private val tokenService = mockk<TokenService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        RecoveryCodeLoginService(
            mfaChallengeRepository,
            userRepository,
            twoFactorRepository,
            recoveryCodeRepository,
            passwordHasher,
            tokenService,
            refreshTokenRepository,
            emailService,
        )

    private val user = TwoFactorTestFixtures.user
    private val storedCode = TwoFactorTestFixtures.recoveryCode("recovery-hash")
    private val command =
        ConsumeRecoveryCodeCommand(mfaToken = "mfa-token", recoveryCode = "cascade tulipe marteau renard")

    private fun givenIssuedChallengeWithOneUnusedCode() {
        mfaChallengeRepository.challengeOwner = user.id
        every { userRepository.findById(user.id) } returns user
        every { recoveryCodeRepository.findUnusedByUserId(user.id) } returns listOf(storedCode)
        every {
            passwordHasher.matches("cascade tulipe marteau renard", HashedPassword("recovery-hash"))
        } returns true
        every { tokenService.generateAccessToken(user) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604_800)
    }

    @Test
    fun `should issue tokens when the recovery code matches an unused code`() {
        givenIssuedChallengeWithOneUnusedCode()

        val result = sut.consume(command)

        assertIs<ConsumeRecoveryCodeResult.Success>(result)
        assertEquals("jwt-token", result.accessToken.value)
        assertEquals("refresh-token-value", result.refreshToken.value)
    }

    @Test
    fun `should mark the code used then tear down two-factor entirely`() {
        givenIssuedChallengeWithOneUnusedCode()

        sut.consume(command)

        verify { recoveryCodeRepository.markUsed(storedCode.id) }
        verify { twoFactorRepository.deleteByUserId(user.id) }
        verify { recoveryCodeRepository.deleteByUserId(user.id) }
        verify { emailService.sendTwoFactorDisabled(user.email) }
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.deletedTokens)
    }

    @Test
    fun `should normalize the submitted code before comparing it to the stored hashes`() {
        givenIssuedChallengeWithOneUnusedCode()

        val result = sut.consume(command.copy(recoveryCode = " Cascade-TULIPÉ_marteau  Renard  "))

        assertIs<ConsumeRecoveryCodeResult.Success>(result)
    }

    @Test
    fun `should return InvalidRecoveryCode when no unused code matches`() {
        mfaChallengeRepository.challengeOwner = user.id
        every { userRepository.findById(user.id) } returns user
        every { recoveryCodeRepository.findUnusedByUserId(user.id) } returns emptyList()

        assertEquals(ConsumeRecoveryCodeResult.InvalidRecoveryCode, sut.consume(command))
        verify(exactly = 0) { twoFactorRepository.deleteByUserId(any()) }
        verify(exactly = 0) { emailService.sendTwoFactorDisabled(user.email) }
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.incrementedTokens)
        assertTrue(mfaChallengeRepository.deletedTokens.isEmpty())
    }

    @Test
    fun `should destroy the challenge and return TooManyAttempts on the fifth failed attempt`() {
        mfaChallengeRepository.challengeOwner = user.id
        mfaChallengeRepository.nextAttemptCount = 5
        every { userRepository.findById(user.id) } returns user
        every { recoveryCodeRepository.findUnusedByUserId(user.id) } returns emptyList()

        assertEquals(ConsumeRecoveryCodeResult.TooManyAttempts, sut.consume(command))
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.deletedTokens)
    }

    @Test
    fun `should return ChallengeExpired when the token is unknown or expired`() {
        mfaChallengeRepository.challengeOwner = null

        assertEquals(ConsumeRecoveryCodeResult.ChallengeExpired, sut.consume(command))
    }
}

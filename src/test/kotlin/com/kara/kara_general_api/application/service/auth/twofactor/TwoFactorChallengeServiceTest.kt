package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.port.input.auth.twofactor.VerifyTwoFactorChallengeCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.VerifyTwoFactorChallengeResult
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TokenService
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwoFactorChallengeServiceTest {
    private val mfaChallengeRepository = FakeMfaChallengeRepository()
    private val userRepository = mockk<UserRepository>()
    private val twoFactorRepository = mockk<TwoFactorRepository>(relaxed = true)
    private val totpService = mockk<TotpService>()
    private val secretCipher = mockk<SecretCipher>()
    private val tokenService = mockk<TokenService>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val sut =
        TwoFactorChallengeService(
            mfaChallengeRepository,
            userRepository,
            twoFactorRepository,
            totpService,
            secretCipher,
            tokenService,
            refreshTokenRepository,
        )

    private val user = TwoFactorTestFixtures.user
    private val command = VerifyTwoFactorChallengeCommand(mfaToken = "mfa-token", code = "123456")

    private fun givenIssuedChallenge(lastUsedStep: Long? = null) {
        mfaChallengeRepository.challengeOwner = user.id
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns
            TwoFactorTestFixtures.activeSecret(lastUsedStep = lastUsedStep)
        every { secretCipher.decrypt("cipher") } returns "PLAINSECRET"
        every { totpService.currentStep() } returns 1_000L
        every { tokenService.generateAccessToken(user) } returns AccessToken("jwt-token", 900)
        every { refreshTokenRepository.issue(user.id) } returns RefreshToken("refresh-token-value", 604_800)
    }

    @Test
    fun `should issue access and refresh tokens when the code is valid`() {
        givenIssuedChallenge()
        every { totpService.verify("PLAINSECRET", command.code) } returns true

        val result = sut.verify(command)

        assertIs<VerifyTwoFactorChallengeResult.Success>(result)
        assertEquals("jwt-token", result.accessToken.value)
        assertEquals("refresh-token-value", result.refreshToken.value)
        assertEquals(user, result.user)
    }

    @Test
    fun `should consume the challenge and the time step when the code is valid`() {
        givenIssuedChallenge()
        every { totpService.verify("PLAINSECRET", command.code) } returns true

        sut.verify(command)

        verify { twoFactorRepository.updateLastUsedStep(user.id, 1_000L) }
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.deletedTokens)
    }

    @Test
    fun `should reject a replayed code whose time step was already consumed`() {
        givenIssuedChallenge(lastUsedStep = 1_000L)
        every { totpService.verify("PLAINSECRET", command.code) } returns true

        assertEquals(VerifyTwoFactorChallengeResult.InvalidCode, sut.verify(command))
        verify(exactly = 0) { twoFactorRepository.updateLastUsedStep(any(), any()) }
        assertTrue(mfaChallengeRepository.deletedTokens.isEmpty())
    }

    @Test
    fun `should return InvalidCode and count the attempt when the code is wrong`() {
        givenIssuedChallenge()
        every { totpService.verify("PLAINSECRET", command.code) } returns false
        mfaChallengeRepository.nextAttemptCount = 3

        assertEquals(VerifyTwoFactorChallengeResult.InvalidCode, sut.verify(command))
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.incrementedTokens)
        assertTrue(mfaChallengeRepository.deletedTokens.isEmpty())
    }

    @Test
    fun `should destroy the challenge and return TooManyAttempts on the fifth failed attempt`() {
        givenIssuedChallenge()
        every { totpService.verify("PLAINSECRET", command.code) } returns false
        mfaChallengeRepository.nextAttemptCount = 5

        assertEquals(VerifyTwoFactorChallengeResult.TooManyAttempts, sut.verify(command))
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.deletedTokens)
    }

    @Test
    fun `should return ChallengeExpired when the token is unknown or expired`() {
        mfaChallengeRepository.challengeOwner = null

        assertEquals(VerifyTwoFactorChallengeResult.ChallengeExpired, sut.verify(command))
    }

    @Test
    fun `should return ChallengeExpired and drop the challenge when two-factor vanished meanwhile`() {
        mfaChallengeRepository.challengeOwner = user.id
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns null

        assertEquals(VerifyTwoFactorChallengeResult.ChallengeExpired, sut.verify(command))
        assertEquals(listOf("mfa-token"), mfaChallengeRepository.deletedTokens)
    }

    @Test
    fun `should never issue a token when the code is wrong`() {
        givenIssuedChallenge()
        every { totpService.verify("PLAINSECRET", command.code) } returns false

        sut.verify(command)

        verify(exactly = 0) { tokenService.generateAccessToken(any()) }
        verify(exactly = 0) { refreshTokenRepository.issue(any()) }
    }
}

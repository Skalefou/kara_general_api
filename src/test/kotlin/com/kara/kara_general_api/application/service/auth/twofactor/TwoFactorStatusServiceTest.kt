package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusResult
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwoFactorStatusServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val twoFactorRepository = mockk<TwoFactorRepository>()
    private val recoveryCodeRepository = mockk<RecoveryCodeRepository>()
    private val sut = TwoFactorStatusService(userRepository, twoFactorRepository, recoveryCodeRepository)

    private val user = TwoFactorTestFixtures.user
    private val command = GetTwoFactorStatusCommand(userId = user.id)

    @Test
    fun `should report disabled and zero remaining codes when no secret exists`() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns null

        val result = sut.status(command)

        assertIs<GetTwoFactorStatusResult.Success>(result)
        assertFalse(result.enabled)
        assertFalse(result.pendingSetup)
        assertEquals(0, result.remainingRecoveryCodes)
        verify(exactly = 0) { recoveryCodeRepository.countUnused(any()) }
    }

    @Test
    fun `should report pendingSetup when a secret exists but was never confirmed`() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.pendingSecret()

        val result = sut.status(command)

        assertIs<GetTwoFactorStatusResult.Success>(result)
        assertFalse(result.enabled)
        assertTrue(result.pendingSetup)
        assertEquals(0, result.remainingRecoveryCodes)
    }

    @Test
    fun `should report enabled with the remaining recovery code count when two-factor is active`() {
        every { userRepository.findById(user.id) } returns user
        every { twoFactorRepository.findByUserId(user.id) } returns TwoFactorTestFixtures.activeSecret()
        every { recoveryCodeRepository.countUnused(user.id) } returns 7

        val result = sut.status(command)

        assertIs<GetTwoFactorStatusResult.Success>(result)
        assertTrue(result.enabled)
        assertFalse(result.pendingSetup)
        assertEquals(7, result.remainingRecoveryCodes)
    }

    @Test
    fun `should return UserNotFound when no account matches the identifier`() {
        every { userRepository.findById(user.id) } returns null

        assertEquals(GetTwoFactorStatusResult.UserNotFound, sut.status(command))
    }
}

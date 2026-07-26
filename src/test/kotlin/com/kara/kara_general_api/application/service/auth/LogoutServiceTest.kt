package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.port.input.auth.LogoutCommand
import com.kara.kara_general_api.domain.port.output.RefreshTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class LogoutServiceTest {
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val sut = LogoutService(refreshTokenRepository)

    @Test
    fun `should revoke the refresh token`() {
        val command = LogoutCommand(refreshToken = "some-refresh-token")
        every { refreshTokenRepository.revoke(command.refreshToken) } returns Unit

        sut.logout(command)

        verify { refreshTokenRepository.revoke("some-refresh-token") }
    }
}

package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class RegisterFcmTokenServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val sut = RegisterFcmTokenService(userRepository)

    @Test
    fun `should delegate token registration to the repository`() {
        val userId = UserId(UUID.randomUUID())
        every { userRepository.updateFcmToken(userId, "device-token") } just Runs

        sut.registerFcmToken(userId, "device-token")

        verify(exactly = 1) { userRepository.updateFcmToken(userId, "device-token") }
    }
}

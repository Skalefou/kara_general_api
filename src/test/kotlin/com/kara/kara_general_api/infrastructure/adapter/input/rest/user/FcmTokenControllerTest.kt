package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.user.RegisterFcmTokenUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val USER_ID = "550e8400-e29b-41d4-a716-446655440000"

@WebMvcTest(FcmTokenController::class)
@Import(SecurityConfig::class)
class FcmTokenControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 204 when the token is registered`() {
        every { registerFcmTokenUseCase.registerFcmToken(UserId(java.util.UUID.fromString(USER_ID)), "device-token") } just Runs

        mockMvc
            .perform(
                post("/api/v1/users/me/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"token": "device-token"}"""),
            ).andExpect(status().isNoContent)

        verify(exactly = 1) {
            registerFcmTokenUseCase.registerFcmToken(UserId(java.util.UUID.fromString(USER_ID)), "device-token")
        }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"token": "device-token"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 when the token is blank`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"token": ""}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val USER_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val REQUEST_BODY = """{"password": "Azerty123"}"""

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 204 when account is deleted`() {
        every { deleteAccountUseCase.deleteAccount(any()) } returns DeleteAccountResult.Success

        mockMvc.perform(
            delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 401 when password is incorrect`() {
        every { deleteAccountUseCase.deleteAccount(any()) } returns DeleteAccountResult.InvalidPassword

        mockMvc.perform(
            delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when user is not found`() {
        every { deleteAccountUseCase.deleteAccount(any()) } returns DeleteAccountResult.UserNotFound

        mockMvc.perform(
            delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc.perform(
            delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        ).andExpect(status().isUnauthorized)
    }
}

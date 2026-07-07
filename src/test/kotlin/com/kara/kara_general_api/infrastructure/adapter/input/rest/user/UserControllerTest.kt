package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountUseCase
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private const val USER_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val REQUEST_BODY = """{"password": "Azerty123"}"""

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    @MockkBean
    private lateinit var updateProfileUseCase: UpdateProfileUseCase

    private val user =
        User(
            id = UserId(UUID.fromString(USER_ID)),
            email = Email("jane.doe@example.com"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("+33612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = true,
        )

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

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 when profile is updated`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.Success(user)

        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName": "Janet"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Jane"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when profile update targets an unknown user`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.UserNotFound

        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName": "Janet"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when the new email is already used`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.EmailAlreadyUsed

        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "taken@example.com"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 when the phone number is invalid`() {
        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"phoneNumber": "invalid"}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 401 when updating profile unauthenticated`() {
        mockMvc.perform(
            patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName": "Janet"}"""),
        ).andExpect(status().isUnauthorized)
    }
}

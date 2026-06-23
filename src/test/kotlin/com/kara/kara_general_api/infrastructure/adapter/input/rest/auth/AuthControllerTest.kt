package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.input.auth.RegisterUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
@WithAnonymousUser
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var registerUseCase: RegisterUseCase

    private val requestBody =
        """
        {
          "email": "client@kara.app",
          "password": "Azerty123",
          "firstName": "Marie",
          "lastName": "Dupont",
          "phoneNumber": "0612345678",
          "birthDate": "1995-05-20"
        }
        """.trimIndent()

    @Test
    fun `should return 201 when registration succeeds`() {
        val user =
            User(
                id = UserId(UUID.randomUUID()),
                email = Email("client@kara.app"),
                hashedPassword = HashedPassword("hashed"),
                firstName = "Marie",
                lastName = "Dupont",
                phoneNumber = PhoneNumber("0612345678"),
                birthDate = LocalDate.of(1995, 5, 20),
                role = UserRole.CLIENT,
                firebaseUid = "firebase-uid",
                createdAt = Instant.now(),
            )
        every { registerUseCase.register(any()) } returns RegisterResult.Success(user)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("client@kara.app"))
    }

    @Test
    fun `should return 409 when email is already used`() {
        every { registerUseCase.register(any()) } returns RegisterResult.EmailAlreadyUsed

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
    }
}

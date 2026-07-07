package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ChangePasswordUseCase
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ForgotPasswordUseCase
import com.kara.kara_general_api.domain.port.input.auth.LoginResult
import com.kara.kara_general_api.domain.port.input.auth.LoginUseCase
import com.kara.kara_general_api.domain.port.input.auth.LogoutUseCase
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenResult
import com.kara.kara_general_api.domain.port.input.auth.RefreshTokenUseCase
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.input.auth.RegisterUseCase
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordResult
import com.kara.kara_general_api.domain.port.input.auth.ResetPasswordUseCase
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailResult
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailUseCase
import com.kara.kara_general_api.domain.port.output.AccessToken
import com.kara.kara_general_api.domain.port.output.RefreshToken
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
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

    @MockkBean
    private lateinit var loginUseCase: LoginUseCase

    @MockkBean
    private lateinit var verifyEmailUseCase: VerifyEmailUseCase

    @MockkBean
    private lateinit var forgotPasswordUseCase: ForgotPasswordUseCase

    @MockkBean
    private lateinit var resetPasswordUseCase: ResetPasswordUseCase

    @MockkBean
    private lateinit var refreshTokenUseCase: RefreshTokenUseCase

    @MockkBean
    private lateinit var logoutUseCase: LogoutUseCase

    @MockkBean
    private lateinit var changePasswordUseCase: ChangePasswordUseCase

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

    private val user =
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
            emailVerified = true,
        )

    private val loginRequestBody =
        """
        {
          "identifiant": "client@kara.app",
          "password": "Azerty123",
          "isEmail": true
        }
        """.trimIndent()

    @Test
    fun `should return 200 with access token and user when login succeeds`() {
        every { loginUseCase.login(any()) } returns
            LoginResult.Success(
                user,
                AccessToken(value = "jwt-token", expiresInSeconds = 900),
                RefreshToken(value = "refresh-token-value", expiresInSeconds = 604800),
                mustChangePassword = false,
            )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
            .andExpect(jsonPath("$.refreshTokenExpiresIn").value(604800))
            .andExpect(jsonPath("$.user.email").value("client@kara.app"))
            .andExpect(jsonPath("$.user.password_hash").doesNotExist())
            .andExpect(jsonPath("$.mustChangePassword").value(false))
    }

    @Test
    fun `should return 403 when the account has been deactivated`() {
        every { loginUseCase.login(any()) } returns LoginResult.AccountDeactivated

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"))
    }

    @Test
    fun `should return 403 when the temporary password has expired`() {
        every { loginUseCase.login(any()) } returns LoginResult.TempPasswordExpired

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("TEMP_PASSWORD_EXPIRED"))
    }

    @Test
    fun `should return 404 when no account matches the login identifier`() {
        every { loginUseCase.login(any()) } returns LoginResult.UserNotFound

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should return 401 when login credentials are invalid`() {
        every { loginUseCase.login(any()) } returns LoginResult.InvalidCredentials

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `should return 410 when the account has been deleted`() {
        every { loginUseCase.login(any()) } returns LoginResult.AccountDeleted

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody),
        )
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.code").value("ACCOUNT_DELETED"))
    }

    private val verifyEmailRequestBody =
        """
        {
          "email": "client@kara.app",
          "code": "123456"
        }
        """.trimIndent()

    @Test
    fun `should return 200 with access token when verification code is valid`() {
        every { verifyEmailUseCase.verify(any()) } returns
            VerifyEmailResult.Success(
                AccessToken(value = "jwt-token", expiresInSeconds = 900),
                RefreshToken(value = "refresh-token-value", expiresInSeconds = 604800),
            )

        mockMvc.perform(
            post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
            .andExpect(jsonPath("$.refreshTokenExpiresIn").value(604800))
    }

    @Test
    fun `should return 400 when verification code is invalid`() {
        every { verifyEmailUseCase.verify(any()) } returns VerifyEmailResult.InvalidCode

        mockMvc.perform(
            post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
    }

    @Test
    fun `should return 404 when no account matches the email`() {
        every { verifyEmailUseCase.verify(any()) } returns VerifyEmailResult.UserNotFound

        mockMvc.perform(
            post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should return 409 when email is already verified`() {
        every { verifyEmailUseCase.verify(any()) } returns VerifyEmailResult.AlreadyVerified

        mockMvc.perform(
            post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_VERIFIED"))
    }

    @Test
    fun `should return 204 when forgot password request is sent`() {
        every { forgotPasswordUseCase.requestReset(any()) } returns ForgotPasswordResult.Success

        mockMvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "client@kara.app"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `should return 204 when password is reset successfully`() {
        every { resetPasswordUseCase.resetPassword(any()) } returns ResetPasswordResult.Success

        mockMvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "client@kara.app", "code": "123456", "newPassword": "Azerty123"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `should return 400 when reset code is expired`() {
        every { resetPasswordUseCase.resetPassword(any()) } returns ResetPasswordResult.CodeExpiredOrMissing

        mockMvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "client@kara.app", "code": "123456", "newPassword": "Azerty123"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("RESET_CODE_EXPIRED"))
    }

    @Test
    fun `should return 400 when reset code is invalid`() {
        every { resetPasswordUseCase.resetPassword(any()) } returns ResetPasswordResult.InvalidCode

        mockMvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "client@kara.app", "code": "000000", "newPassword": "Azerty123"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_RESET_CODE"))
    }

    @Test
    fun `should return 404 when user is not found during reset`() {
        every { resetPasswordUseCase.resetPassword(any()) } returns ResetPasswordResult.UserNotFound

        mockMvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "unknown@kara.app", "code": "123456", "newPassword": "Azerty123"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should return 200 with new tokens when refresh succeeds`() {
        every { refreshTokenUseCase.refresh(any()) } returns
            RefreshTokenResult.Success(
                AccessToken(value = "new-jwt-token", expiresInSeconds = 900),
                RefreshToken(value = "new-refresh-token", expiresInSeconds = 604800),
            )

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "old-refresh-token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-jwt-token"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.refreshTokenExpiresIn").value(604800))
    }

    @Test
    fun `should return 401 when refresh token is invalid`() {
        every { refreshTokenUseCase.refresh(any()) } returns RefreshTokenResult.InvalidToken

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "unknown-token"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
    }

    @Test
    fun `should return 204 when logout is called`() {
        every { logoutUseCase.logout(any()) } returns Unit

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "some-refresh-token"}"""),
        ).andExpect(status().isNoContent)
    }

    private val changePasswordRequestBody =
        """{"currentPassword": "OldP@ssw0rd", "newPassword": "N3wStr0ngP@ssw0rd"}"""

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    fun `should return 204 when password is changed successfully`() {
        every { changePasswordUseCase.changePassword(any()) } returns ChangePasswordResult.Success

        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(changePasswordRequestBody),
        ).andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    fun `should return 401 when the current password is incorrect`() {
        every { changePasswordUseCase.changePassword(any()) } returns ChangePasswordResult.InvalidCurrentPassword

        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(changePasswordRequestBody),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    fun `should return 400 when the new password is too weak`() {
        every { changePasswordUseCase.changePassword(any()) } returns
            ChangePasswordResult.WeakPassword(listOf("Le mot de passe doit contenir au moins 8 caractères"))

        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(changePasswordRequestBody),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
    }

    @Test
    fun `should return 401 when changing password unauthenticated`() {
        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(changePasswordRequestBody),
        ).andExpect(status().isUnauthorized)
    }
}

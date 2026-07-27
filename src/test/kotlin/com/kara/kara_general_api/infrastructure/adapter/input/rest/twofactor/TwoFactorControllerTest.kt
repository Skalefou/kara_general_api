package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor

import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

private const val USER_ID = "11111111-1111-1111-1111-111111111111"
private const val BASE_PATH = "/api/v1/users/me/two-factor"

@WebMvcTest(TwoFactorController::class)
@Import(SecurityConfig::class)
@WithMockUser(username = USER_ID)
class TwoFactorControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getTwoFactorStatusUseCase: GetTwoFactorStatusUseCase

    @MockkBean
    private lateinit var setupTwoFactorUseCase: SetupTwoFactorUseCase

    @MockkBean
    private lateinit var activateTwoFactorUseCase: ActivateTwoFactorUseCase

    @MockkBean
    private lateinit var regenerateRecoveryCodesUseCase: RegenerateRecoveryCodesUseCase

    @MockkBean
    private lateinit var disableTwoFactorUseCase: DisableTwoFactorUseCase

    private val passwordBody = """{"password": "S3cur3P@ssw0rd"}"""
    private val codeBody = """{"code": "123456"}"""
    private val passwordAndCodeBody = """{"password": "S3cur3P@ssw0rd", "code": "123456"}"""
    private val recoveryCodes = listOf("cascade tulipe marteau renard", "biscuit orage colline banjo")

    // ----- GET (racine) -------------------------------------------------------

    @Test
    fun `should return 200 with the two-factor status`() {
        every { getTwoFactorStatusUseCase.status(any()) } returns
            GetTwoFactorStatusResult.Success(enabled = true, pendingSetup = false, remainingRecoveryCodes = 7)

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.pendingSetup").value(false))
            .andExpect(jsonPath("$.remainingRecoveryCodes").value(7))
    }

    @Test
    fun `should pass the authenticated user id to the status use case`() {
        every { getTwoFactorStatusUseCase.status(any()) } answers {
            val command = firstArg<com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusCommand>()
            if (command.userId.value == UUID.fromString(USER_ID)) {
                GetTwoFactorStatusResult.Success(enabled = false, pendingSetup = false, remainingRecoveryCodes = 0)
            } else {
                GetTwoFactorStatusResult.UserNotFound
            }
        }

        mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk)
    }

    @Test
    fun `should return 404 when the account behind the status request no longer exists`() {
        every { getTwoFactorStatusUseCase.status(any()) } returns GetTwoFactorStatusResult.UserNotFound

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    @WithAnonymousUser
    fun `should return 401 when the status is requested without authentication`() {
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isUnauthorized)
    }

    // ----- POST /setup --------------------------------------------------------

    @Test
    fun `should return 201 with the secret and the otpauth uri when setup succeeds`() {
        every { setupTwoFactorUseCase.setup(any()) } returns
            SetupTwoFactorResult.Success(secret = "JBSWY3DPEHPK3PXP", otpauthUri = "otpauth://totp/Kara:x")

        mockMvc
            .perform(post("$BASE_PATH/setup").contentType(MediaType.APPLICATION_JSON).content(passwordBody))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"))
            .andExpect(jsonPath("$.otpauthUri").value("otpauth://totp/Kara:x"))
    }

    @Test
    fun `should return 403 with INVALID_CURRENT_PASSWORD when the setup password is wrong`() {
        every { setupTwoFactorUseCase.setup(any()) } returns SetupTwoFactorResult.InvalidPassword

        mockMvc
            .perform(post("$BASE_PATH/setup").contentType(MediaType.APPLICATION_JSON).content(passwordBody))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
    }

    @Test
    fun `should return 409 when setup is attempted while two-factor is already enabled`() {
        every { setupTwoFactorUseCase.setup(any()) } returns SetupTwoFactorResult.AlreadyEnabled

        mockMvc
            .perform(post("$BASE_PATH/setup").contentType(MediaType.APPLICATION_JSON).content(passwordBody))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("TWO_FACTOR_ALREADY_ENABLED"))
    }

    @Test
    fun `should return 404 when the account behind the setup request no longer exists`() {
        every { setupTwoFactorUseCase.setup(any()) } returns SetupTwoFactorResult.UserNotFound

        mockMvc
            .perform(post("$BASE_PATH/setup").contentType(MediaType.APPLICATION_JSON).content(passwordBody))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should return 400 when the setup password is blank`() {
        mockMvc
            .perform(post("$BASE_PATH/setup").contentType(MediaType.APPLICATION_JSON).content("""{"password": ""}"""))
            .andExpect(status().isBadRequest)
    }

    // ----- POST /activate -----------------------------------------------------

    @Test
    fun `should return 200 with the recovery codes when activation succeeds`() {
        every { activateTwoFactorUseCase.activate(any()) } returns ActivateTwoFactorResult.Success(recoveryCodes)

        mockMvc
            .perform(post("$BASE_PATH/activate").contentType(MediaType.APPLICATION_JSON).content(codeBody))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.recoveryCodes.length()").value(2))
            .andExpect(jsonPath("$.recoveryCodes[0]").value("cascade tulipe marteau renard"))
    }

    @Test
    fun `should return 400 with INVALID_TWO_FACTOR_CODE when the activation code is wrong`() {
        every { activateTwoFactorUseCase.activate(any()) } returns ActivateTwoFactorResult.InvalidCode

        mockMvc
            .perform(post("$BASE_PATH/activate").contentType(MediaType.APPLICATION_JSON).content(codeBody))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_TWO_FACTOR_CODE"))
    }

    @Test
    fun `should return 409 with TWO_FACTOR_SETUP_NOT_FOUND when no setup is pending`() {
        every { activateTwoFactorUseCase.activate(any()) } returns ActivateTwoFactorResult.SetupNotFound

        mockMvc
            .perform(post("$BASE_PATH/activate").contentType(MediaType.APPLICATION_JSON).content(codeBody))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("TWO_FACTOR_SETUP_NOT_FOUND"))
    }

    @Test
    fun `should return 409 when activation is attempted while two-factor is already enabled`() {
        every { activateTwoFactorUseCase.activate(any()) } returns ActivateTwoFactorResult.AlreadyEnabled

        mockMvc
            .perform(post("$BASE_PATH/activate").contentType(MediaType.APPLICATION_JSON).content(codeBody))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("TWO_FACTOR_ALREADY_ENABLED"))
    }

    @Test
    fun `should return 404 when the account behind the activation request no longer exists`() {
        every { activateTwoFactorUseCase.activate(any()) } returns ActivateTwoFactorResult.UserNotFound

        mockMvc
            .perform(post("$BASE_PATH/activate").contentType(MediaType.APPLICATION_JSON).content(codeBody))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    // ----- POST /recovery-codes ----------------------------------------------

    @Test
    fun `should return 200 with a fresh series when recovery codes are regenerated`() {
        every { regenerateRecoveryCodesUseCase.regenerate(any()) } returns
            RegenerateRecoveryCodesResult.Success(recoveryCodes)

        mockMvc
            .perform(
                post("$BASE_PATH/recovery-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordAndCodeBody),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.recoveryCodes.length()").value(2))
    }

    @Test
    fun `should return 403 with INVALID_CURRENT_PASSWORD when the regeneration password is wrong`() {
        every { regenerateRecoveryCodesUseCase.regenerate(any()) } returns
            RegenerateRecoveryCodesResult.InvalidPassword

        mockMvc
            .perform(
                post("$BASE_PATH/recovery-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordAndCodeBody),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
    }

    @Test
    fun `should return 400 when the regeneration code is wrong`() {
        every { regenerateRecoveryCodesUseCase.regenerate(any()) } returns
            RegenerateRecoveryCodesResult.InvalidCode

        mockMvc
            .perform(
                post("$BASE_PATH/recovery-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordAndCodeBody),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_TWO_FACTOR_CODE"))
    }

    @Test
    fun `should return 409 with TWO_FACTOR_NOT_ENABLED when regenerating without active two-factor`() {
        every { regenerateRecoveryCodesUseCase.regenerate(any()) } returns
            RegenerateRecoveryCodesResult.NotEnabled

        mockMvc
            .perform(
                post("$BASE_PATH/recovery-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordAndCodeBody),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("TWO_FACTOR_NOT_ENABLED"))
    }

    @Test
    fun `should return 404 when the account behind the regeneration request no longer exists`() {
        every { regenerateRecoveryCodesUseCase.regenerate(any()) } returns
            RegenerateRecoveryCodesResult.UserNotFound

        mockMvc
            .perform(
                post("$BASE_PATH/recovery-codes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordAndCodeBody),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    // ----- DELETE (racine) ---------------------------------------------------

    @Test
    fun `should return 204 when two-factor is disabled`() {
        every { disableTwoFactorUseCase.disable(any()) } returns DisableTwoFactorResult.Success

        mockMvc
            .perform(delete(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(passwordAndCodeBody))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 403 with INVALID_CURRENT_PASSWORD when the disable password is wrong`() {
        every { disableTwoFactorUseCase.disable(any()) } returns DisableTwoFactorResult.InvalidPassword

        mockMvc
            .perform(delete(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(passwordAndCodeBody))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
    }

    @Test
    fun `should return 400 when the disable code is neither a valid totp code nor a recovery code`() {
        every { disableTwoFactorUseCase.disable(any()) } returns DisableTwoFactorResult.InvalidCode

        mockMvc
            .perform(delete(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(passwordAndCodeBody))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_TWO_FACTOR_CODE"))
    }

    @Test
    fun `should return 409 with TWO_FACTOR_NOT_ENABLED when disabling an inactive two-factor`() {
        every { disableTwoFactorUseCase.disable(any()) } returns DisableTwoFactorResult.NotEnabled

        mockMvc
            .perform(delete(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(passwordAndCodeBody))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("TWO_FACTOR_NOT_ENABLED"))
    }

    @Test
    fun `should return 404 when the account behind the disable request no longer exists`() {
        every { disableTwoFactorUseCase.disable(any()) } returns DisableTwoFactorResult.UserNotFound

        mockMvc
            .perform(delete(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(passwordAndCodeBody))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    @WithAnonymousUser
    fun `should return 401 when disabling without authentication`() {
        mockMvc
            .perform(delete(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(passwordAndCodeBody))
            .andExpect(status().isUnauthorized)
    }
}

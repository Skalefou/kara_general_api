package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.ActivateTwoFactorUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.DisableTwoFactorUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.GetTwoFactorStatusUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.RegenerateRecoveryCodesUseCase
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorCommand
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorResult
import com.kara.kara_general_api.domain.port.input.auth.twofactor.SetupTwoFactorUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.DisableTwoFactorRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.RecoveryCodesResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.RegenerateRecoveryCodesRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorActivateRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorSetupRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorSetupResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorStatusResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users/me/two-factor")
class TwoFactorController(
    private val getTwoFactorStatusUseCase: GetTwoFactorStatusUseCase,
    private val setupTwoFactorUseCase: SetupTwoFactorUseCase,
    private val activateTwoFactorUseCase: ActivateTwoFactorUseCase,
    private val regenerateRecoveryCodesUseCase: RegenerateRecoveryCodesUseCase,
    private val disableTwoFactorUseCase: DisableTwoFactorUseCase,
) : TwoFactorApi {
    override fun status(authentication: Authentication): ResponseEntity<Any> {
        val command = GetTwoFactorStatusCommand(userId = currentUserId(authentication))

        return when (val result = getTwoFactorStatusUseCase.status(command)) {
            is GetTwoFactorStatusResult.Success ->
                ResponseEntity.ok(
                    TwoFactorStatusResponse(
                        enabled = result.enabled,
                        pendingSetup = result.pendingSetup,
                        remainingRecoveryCodes = result.remainingRecoveryCodes,
                    ),
                )

            GetTwoFactorStatusResult.UserNotFound -> userNotFoundProblem()
        }
    }

    override fun setup(
        request: TwoFactorSetupRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            SetupTwoFactorCommand(
                userId = currentUserId(authentication),
                password = request.password,
            )

        return when (val result = setupTwoFactorUseCase.setup(command)) {
            is SetupTwoFactorResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(
                    TwoFactorSetupResponse(secret = result.secret, otpauthUri = result.otpauthUri),
                )

            SetupTwoFactorResult.UserNotFound -> userNotFoundProblem()

            SetupTwoFactorResult.InvalidPassword -> invalidCurrentPasswordProblem()

            SetupTwoFactorResult.AlreadyEnabled -> alreadyEnabledProblem()
        }
    }

    override fun activate(
        request: TwoFactorActivateRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            ActivateTwoFactorCommand(userId = currentUserId(authentication), code = request.code)

        return when (val result = activateTwoFactorUseCase.activate(command)) {
            is ActivateTwoFactorResult.Success ->
                ResponseEntity.ok(RecoveryCodesResponse(recoveryCodes = result.recoveryCodes))

            ActivateTwoFactorResult.UserNotFound -> userNotFoundProblem()

            ActivateTwoFactorResult.AlreadyEnabled -> alreadyEnabledProblem()

            ActivateTwoFactorResult.SetupNotFound ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail
                        .forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            "Aucune configuration d'authentification forte n'est en attente. " +
                                "Recommencez la mise en place.",
                        ).apply {
                            title = "Aucune configuration en attente"
                            setProperty("code", "TWO_FACTOR_SETUP_NOT_FOUND")
                        },
                )

            ActivateTwoFactorResult.InvalidCode -> invalidCodeProblem()
        }
    }

    override fun regenerateRecoveryCodes(
        request: RegenerateRecoveryCodesRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            RegenerateRecoveryCodesCommand(
                userId = currentUserId(authentication),
                password = request.password,
                code = request.code,
            )

        return when (val result = regenerateRecoveryCodesUseCase.regenerate(command)) {
            is RegenerateRecoveryCodesResult.Success ->
                ResponseEntity.ok(RecoveryCodesResponse(recoveryCodes = result.recoveryCodes))

            RegenerateRecoveryCodesResult.UserNotFound -> userNotFoundProblem()

            RegenerateRecoveryCodesResult.InvalidPassword -> invalidCurrentPasswordProblem()

            RegenerateRecoveryCodesResult.NotEnabled -> notEnabledProblem()

            RegenerateRecoveryCodesResult.InvalidCode -> invalidCodeProblem()
        }
    }

    override fun disable(
        request: DisableTwoFactorRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            DisableTwoFactorCommand(
                userId = currentUserId(authentication),
                password = request.password,
                code = request.code,
            )

        return when (val result = disableTwoFactorUseCase.disable(command)) {
            DisableTwoFactorResult.Success -> ResponseEntity.noContent().build()

            DisableTwoFactorResult.UserNotFound -> userNotFoundProblem()

            DisableTwoFactorResult.InvalidPassword -> invalidCurrentPasswordProblem()

            DisableTwoFactorResult.NotEnabled -> notEnabledProblem()

            DisableTwoFactorResult.InvalidCode -> invalidCodeProblem()
        }
    }

    private fun currentUserId(authentication: Authentication): UserId = UserId(UUID.fromString(authentication.name))

    private fun userNotFoundProblem(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucun compte ne correspond à cet identifiant.",
                ).apply {
                    title = "Compte introuvable"
                    setProperty("code", "USER_NOT_FOUND")
                },
        )

    /**
     * 403 et non 401 : un 401 déclencherait à tort le refresh-and-retry côté front alors que l'access token
     * est parfaitement valide — c'est le mot de passe saisi dans le formulaire qui est faux.
     */
    private fun invalidCurrentPasswordProblem(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Le mot de passe actuel est incorrect.",
                ).apply {
                    title = "Mot de passe incorrect"
                    setProperty("code", "INVALID_CURRENT_PASSWORD")
                },
        )

    private fun invalidCodeProblem(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Le code fourni est incorrect.",
                ).apply {
                    title = "Code invalide"
                    setProperty("code", "INVALID_TWO_FACTOR_CODE")
                },
        )

    private fun alreadyEnabledProblem(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "L'authentification à deux facteurs est déjà activée sur ce compte.",
                ).apply {
                    title = "Authentification forte déjà active"
                    setProperty("code", "TWO_FACTOR_ALREADY_ENABLED")
                },
        )

    private fun notEnabledProblem(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "L'authentification à deux facteurs n'est pas activée sur ce compte.",
                ).apply {
                    title = "Authentification forte inactive"
                    setProperty("code", "TWO_FACTOR_NOT_ENABLED")
                },
        )
}

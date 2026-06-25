package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.auth.RegisterCommand
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.input.auth.RegisterUseCase
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailCommand
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailResult
import com.kara.kara_general_api.domain.port.input.auth.VerifyEmailUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.RegisterResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto.VerifyEmailResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
) : AuthApi {

    override fun register(request: RegisterRequest): ResponseEntity<Any> {
        val command =
            RegisterCommand(
                email = Email(request.email),
                plainPassword = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                phoneNumber = PhoneNumber(request.phoneNumber),
                birthDate = request.birthDate,
            )

        return when (val result = registerUseCase.register(command)) {
            is RegisterResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(
                    RegisterResponse(
                        id = result.user.id.value,
                        email = result.user.email.value,
                        firstName = result.user.firstName,
                        lastName = result.user.lastName,
                    ),
                )

            RegisterResult.EmailAlreadyUsed ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Un compte existe déjà avec cet email.",
                    ).apply {
                        title = "Email déjà utilisé"
                        setProperty("code", "EMAIL_ALREADY_USED")
                    },
                )

            is RegisterResult.InvalidPassword ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        result.reasons.joinToString(" "),
                    ).apply {
                        title = "Mot de passe invalide"
                        setProperty("code", "INVALID_PASSWORD")
                    },
                )
        }
    }

    override fun verifyEmail(request: VerifyEmailRequest): ResponseEntity<Any> {
        val command = VerifyEmailCommand(email = Email(request.email), code = request.code)

        return when (val result = verifyEmailUseCase.verify(command)) {
            is VerifyEmailResult.Success ->
                ResponseEntity.ok(
                    VerifyEmailResponse(
                        accessToken = result.accessToken.value,
                        expiresIn = result.accessToken.expiresInSeconds,
                    ),
                )

            VerifyEmailResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet email.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )

            VerifyEmailResult.AlreadyVerified ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Cet email a déjà été vérifié.",
                    ).apply {
                        title = "Email déjà vérifié"
                        setProperty("code", "EMAIL_ALREADY_VERIFIED")
                    },
                )

            VerifyEmailResult.CodeExpiredOrMissing ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Le code de vérification a expiré. Merci de redemander un nouveau code.",
                    ).apply {
                        title = "Code expiré"
                        setProperty("code", "VERIFICATION_CODE_EXPIRED")
                    },
                )

            VerifyEmailResult.InvalidCode ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Le code de vérification est incorrect.",
                    ).apply {
                        title = "Code invalide"
                        setProperty("code", "INVALID_VERIFICATION_CODE")
                    },
                )
        }
    }
}

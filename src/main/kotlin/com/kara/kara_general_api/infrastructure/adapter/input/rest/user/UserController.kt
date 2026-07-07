package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountCommand
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountUseCase
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.DeleteAccountRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UpdateProfileRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
) : UserApi {

    override fun deleteAccount(request: DeleteAccountRequest, authentication: Authentication): ResponseEntity<Any> {
        val command =
            DeleteAccountCommand(
                userId = UserId(UUID.fromString(authentication.name)),
                password = request.password,
            )

        return when (deleteAccountUseCase.deleteAccount(command)) {
            DeleteAccountResult.Success ->
                ResponseEntity.noContent().build()

            DeleteAccountResult.InvalidPassword ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "Le mot de passe est incorrect.",
                    ).apply {
                        title = "Mot de passe incorrect"
                        setProperty("code", "INVALID_PASSWORD")
                    },
                )

            DeleteAccountResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet identifiant.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )
        }
    }

    override fun updateProfile(request: UpdateProfileRequest, authentication: Authentication): ResponseEntity<Any> {
        val command =
            UpdateProfileCommand(
                userId = UserId(UUID.fromString(authentication.name)),
                firstName = request.firstName?.requireNotBlank("firstName"),
                lastName = request.lastName?.requireNotBlank("lastName"),
                phoneNumber = request.phoneNumber?.let { PhoneNumber(it) },
                birthDate = request.birthDate,
                email = request.email?.let { Email(it) },
            )

        return when (val result = updateProfileUseCase.updateProfile(command)) {
            is UpdateProfileResult.Success ->
                ResponseEntity.ok(UserResponse.from(result.user))

            UpdateProfileResult.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucun compte ne correspond à cet identifiant.",
                    ).apply {
                        title = "Compte introuvable"
                        setProperty("code", "USER_NOT_FOUND")
                    },
                )

            UpdateProfileResult.EmailAlreadyUsed ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Cette adresse email est déjà utilisée.",
                    ).apply {
                        title = "Email déjà utilisé"
                        setProperty("code", "EMAIL_ALREADY_USED")
                    },
                )
        }
    }

    private fun String.requireNotBlank(field: String): String {
        require(isNotBlank()) { "Le champ $field ne peut pas être vide." }
        return this
    }
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountResult
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountUseCase
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountResult
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountUseCase
import com.kara.kara_general_api.domain.port.input.admin.ListAllAccountsQuery
import com.kara.kara_general_api.domain.port.input.admin.ListAllAccountsUseCase
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountResult
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountUseCase
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountCommand
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountUseCase
import com.kara.kara_general_api.domain.port.input.user.DeleteProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.DeleteProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.input.user.GetProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.GetProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.CreateServerAccountRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.DeleteAccountRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.ProfilePhotoResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UpdateProfileRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserListResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.util.UUID

private val PHOTO_URL_TTL: Duration = Duration.ofMinutes(15)

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val createServerAccountUseCase: CreateServerAccountUseCase,
    private val listAllAccountsUseCase: ListAllAccountsUseCase,
    private val reinviteServerAccountUseCase: ReinviteServerAccountUseCase,
    private val deactivateAccountUseCase: DeactivateAccountUseCase,
    private val updateProfilePhotoUseCase: UpdateProfilePhotoUseCase,
    private val getProfilePhotoUseCase: GetProfilePhotoUseCase,
    private val deleteProfilePhotoUseCase: DeleteProfilePhotoUseCase,
    private val imageStorage: ImageStoragePort,
) : UserApi {

    private fun signedPhotoUrl(key: String): String = imageStorage.signedUrl(key, PHOTO_URL_TTL)

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
                userNotFound()
        }
    }

    override fun updateProfile(request: UpdateProfileRequest, authentication: Authentication): ResponseEntity<Any> =
        updateProfile(UserId(UUID.fromString(authentication.name)), request)

    override fun uploadProfilePhoto(file: MultipartFile, authentication: Authentication): ResponseEntity<Any> {
        val command =
            UpdateProfilePhotoCommand(
                userId = UserId(UUID.fromString(authentication.name)),
                bytes = file.bytes,
                contentType = file.contentType,
            )

        return when (val result = updateProfilePhotoUseCase.updatePhoto(command)) {
            is UpdateProfilePhotoResult.Success ->
                ResponseEntity.ok(ProfilePhotoResponse(result.photoUrl))

            UpdateProfilePhotoResult.UserNotFound ->
                userNotFound()

            UpdateProfilePhotoResult.InvalidImageType ->
                invalidImageType()

            UpdateProfilePhotoResult.ImageTooLarge ->
                imageTooLarge()
        }
    }

    override fun getProfilePhoto(authentication: Authentication): ResponseEntity<Any> =
        when (val result = getProfilePhotoUseCase.getPhotoUrl(UserId(UUID.fromString(authentication.name)))) {
            is GetProfilePhotoResult.Success ->
                ResponseEntity.ok(ProfilePhotoResponse(result.photoUrl))

            GetProfilePhotoResult.UserNotFound ->
                userNotFound()

            GetProfilePhotoResult.NoPhoto ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucune photo de profil n'a été définie.",
                    ).apply {
                        title = "Photo introuvable"
                        setProperty("code", "PROFILE_PHOTO_NOT_FOUND")
                    },
                )
        }

    override fun deleteProfilePhoto(authentication: Authentication): ResponseEntity<Any> =
        when (deleteProfilePhotoUseCase.deletePhoto(UserId(UUID.fromString(authentication.name)))) {
            DeleteProfilePhotoResult.Success ->
                ResponseEntity.noContent().build()

            DeleteProfilePhotoResult.UserNotFound ->
                userNotFound()
        }

    override fun listAccounts(page: Int, size: Int): ResponseEntity<UserListResponse> {
        val accountPage = listAllAccountsUseCase.listAllAccounts(ListAllAccountsQuery(page = page, size = size))
        return ResponseEntity.ok(UserListResponse.from(accountPage, ::signedPhotoUrl))
    }

    override fun createServerAccount(request: CreateServerAccountRequest): ResponseEntity<Any> {
        val command =
            CreateServerAccountCommand(
                email = Email(request.email),
                firstName = request.firstName,
                lastName = request.lastName,
                phoneNumber = PhoneNumber(request.phoneNumber),
                birthDate = request.birthDate,
            )

        return when (val result = createServerAccountUseCase.createServerAccount(command)) {
            is CreateServerAccountResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.user, ::signedPhotoUrl))

            CreateServerAccountResult.EmailAlreadyUsed ->
                emailAlreadyUsed()
        }
    }

    override fun updateAccount(id: UUID, request: UpdateProfileRequest): ResponseEntity<Any> =
        updateProfile(UserId(id), request)

    override fun reinviteServerAccount(id: UUID): ResponseEntity<Any> =
        when (reinviteServerAccountUseCase.reinvite(ReinviteServerAccountCommand(userId = UserId(id)))) {
            ReinviteServerAccountResult.Success ->
                ResponseEntity.noContent().build()

            ReinviteServerAccountResult.UserNotFound ->
                userNotFound()

            ReinviteServerAccountResult.NotAServer ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Ce compte n'est pas un compte serveur.",
                    ).apply {
                        title = "Compte non serveur"
                        setProperty("code", "NOT_A_SERVER")
                    },
                )
        }

    override fun deactivateAccount(id: UUID): ResponseEntity<Any> =
        when (deactivateAccountUseCase.deactivate(DeactivateAccountCommand(userId = UserId(id)))) {
            DeactivateAccountResult.Success ->
                ResponseEntity.noContent().build()

            DeactivateAccountResult.UserNotFound ->
                userNotFound()

            DeactivateAccountResult.AlreadyDeactivated ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Ce compte est déjà désactivé.",
                    ).apply {
                        title = "Compte déjà désactivé"
                        setProperty("code", "ALREADY_DEACTIVATED")
                    },
                )
        }

    private fun updateProfile(userId: UserId, request: UpdateProfileRequest): ResponseEntity<Any> {
        val command =
            UpdateProfileCommand(
                userId = userId,
                firstName = request.firstName?.requireNotBlank("firstName"),
                lastName = request.lastName?.requireNotBlank("lastName"),
                phoneNumber = request.phoneNumber?.let { PhoneNumber(it) },
                birthDate = request.birthDate,
                email = request.email?.let { Email(it) },
            )

        return when (val result = updateProfileUseCase.updateProfile(command)) {
            is UpdateProfileResult.Success ->
                ResponseEntity.ok(UserResponse.from(result.user, ::signedPhotoUrl))

            UpdateProfileResult.UserNotFound ->
                userNotFound()

            UpdateProfileResult.EmailAlreadyUsed ->
                emailAlreadyUsed()
        }
    }

    private fun userNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Aucun compte ne correspond à cet identifiant.",
            ).apply {
                title = "Compte introuvable"
                setProperty("code", "USER_NOT_FOUND")
            },
        )

    private fun emailAlreadyUsed(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Cette adresse email est déjà utilisée.",
            ).apply {
                title = "Email déjà utilisé"
                setProperty("code", "EMAIL_ALREADY_USED")
            },
        )

    private fun invalidImageType(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Format d'image non supporté. Formats acceptés : JPEG, PNG, WebP, AVIF, HEIC.",
            ).apply {
                title = "Format d'image non supporté"
                setProperty("code", "INVALID_IMAGE_TYPE")
            },
        )

    private fun imageTooLarge(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "L'image dépasse la taille maximale autorisée (5 Mo).",
            ).apply {
                title = "Image trop volumineuse"
                setProperty("code", "IMAGE_TOO_LARGE")
            },
        )

    private fun String.requireNotBlank(field: String): String {
        require(isNotBlank()) { "Le champ $field ne peut pas être vide." }
        return this
    }
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.model.user.PhotoStatus
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.admin.AccountPage
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountResult
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountUseCase
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountResult
import com.kara.kara_general_api.domain.port.input.admin.DeactivateAccountUseCase
import com.kara.kara_general_api.domain.port.input.admin.ListAllAccountsUseCase
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountResult
import com.kara.kara_general_api.domain.port.input.admin.ReinviteServerAccountUseCase
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountResult
import com.kara.kara_general_api.domain.port.input.user.DeleteAccountUseCase
import com.kara.kara_general_api.domain.port.input.user.DeleteProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.DeleteProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.input.user.GetProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.GetProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfileUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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

    @MockkBean
    private lateinit var createServerAccountUseCase: CreateServerAccountUseCase

    @MockkBean
    private lateinit var listAllAccountsUseCase: ListAllAccountsUseCase

    @MockkBean
    private lateinit var reinviteServerAccountUseCase: ReinviteServerAccountUseCase

    @MockkBean
    private lateinit var deactivateAccountUseCase: DeactivateAccountUseCase

    @MockkBean
    private lateinit var updateProfilePhotoUseCase: UpdateProfilePhotoUseCase

    @MockkBean
    private lateinit var getProfilePhotoUseCase: GetProfilePhotoUseCase

    @MockkBean
    private lateinit var deleteProfilePhotoUseCase: DeleteProfilePhotoUseCase

    @MockkBean
    private lateinit var imageStorage: ImageStoragePort

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

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 401 when password is incorrect`() {
        every { deleteAccountUseCase.deleteAccount(any()) } returns DeleteAccountResult.InvalidPassword

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when user is not found`() {
        every { deleteAccountUseCase.deleteAccount(any()) } returns DeleteAccountResult.UserNotFound

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 when profile is updated`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.Success(user)

        mockMvc
            .perform(
                patch("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"firstName": "Janet"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Jane"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when profile update targets an unknown user`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.UserNotFound

        mockMvc
            .perform(
                patch("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"firstName": "Janet"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when the new email is already used`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.EmailAlreadyUsed

        mockMvc
            .perform(
                patch("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "taken@example.com"}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 when the phone number is invalid`() {
        mockMvc
            .perform(
                patch("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"phoneNumber": "invalid"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 401 when updating profile unauthenticated`() {
        mockMvc
            .perform(
                patch("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"firstName": "Janet"}"""),
            ).andExpect(status().isUnauthorized)
    }

    private val createServerRequestBody =
        """
        {
          "email": "server@example.com",
          "firstName": "Jane",
          "lastName": "Doe",
          "phoneNumber": "+33612345678",
          "birthDate": "1990-01-15"
        }
        """.trimIndent()

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 201 when a server account is created`() {
        every { createServerAccountUseCase.createServerAccount(any()) } returns
            CreateServerAccountResult.Success(user.copy(role = UserRole.SERVER))

        mockMvc
            .perform(
                post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createServerRequestBody),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.role").value("SERVER"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 409 when creating a server account with a used email`() {
        every { createServerAccountUseCase.createServerAccount(any()) } returns CreateServerAccountResult.EmailAlreadyUsed

        mockMvc
            .perform(
                post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createServerRequestBody),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should return 403 when a non-admin creates a server account`() {
        mockMvc
            .perform(
                post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createServerRequestBody),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 200 with a page of accounts`() {
        every { listAllAccountsUseCase.listAllAccounts(any()) } returns
            AccountPage(accounts = listOf(user), page = 0, size = 20, totalElements = 1)

        mockMvc
            .perform(get("/api/v1/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.users[0].email").value("jane.doe@example.com"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should expose a signed photo url for accounts that have a photo`() {
        every { listAllAccountsUseCase.listAllAccounts(any()) } returns
            AccountPage(
                accounts =
                    listOf(
                        user.copy(photoStatus = PhotoStatus.READY, photoFullKey = "profiles/x/full.webp"),
                    ),
                page = 0,
                size = 20,
                totalElements = 1,
            )
        every { imageStorage.signedUrl("profiles/x/full.webp", any()) } returns "https://signed.example/x"

        mockMvc
            .perform(get("/api/v1/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users[0].photoUrl").value("https://signed.example/x"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return a null photo url for accounts without a photo`() {
        every { listAllAccountsUseCase.listAllAccounts(any()) } returns
            AccountPage(accounts = listOf(user), page = 0, size = 20, totalElements = 1)

        mockMvc
            .perform(get("/api/v1/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users[0].photoUrl").isEmpty)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 200 when an admin updates an account by id`() {
        every { updateProfileUseCase.updateProfile(any()) } returns UpdateProfileResult.Success(user)

        mockMvc
            .perform(
                patch("/api/v1/users/$USER_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"firstName": "Janet"}"""),
            ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 204 when a server account is reinvited`() {
        every { reinviteServerAccountUseCase.reinvite(any()) } returns ReinviteServerAccountResult.Success

        mockMvc
            .perform(post("/api/v1/users/$USER_ID/reinvite"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 409 when reinviting an account that is not a server`() {
        every { reinviteServerAccountUseCase.reinvite(any()) } returns ReinviteServerAccountResult.NotAServer

        mockMvc
            .perform(post("/api/v1/users/$USER_ID/reinvite"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("NOT_A_SERVER"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 204 when an account is deactivated`() {
        every { deactivateAccountUseCase.deactivate(any()) } returns DeactivateAccountResult.Success

        mockMvc
            .perform(post("/api/v1/users/$USER_ID/deactivate"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["ADMIN"])
    fun `should return 409 when deactivating an already deactivated account`() {
        every { deactivateAccountUseCase.deactivate(any()) } returns DeactivateAccountResult.AlreadyDeactivated

        mockMvc
            .perform(post("/api/v1/users/$USER_ID/deactivate"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ALREADY_DEACTIVATED"))
    }

    @Test
    fun `should return 401 when listing accounts unauthenticated`() {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized)
    }

    private fun photoFile(contentType: String = MediaType.IMAGE_JPEG_VALUE) =
        MockMultipartFile("file", "photo.jpg", contentType, byteArrayOf(1, 2, 3))

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 202 with the image id when a profile photo upload is accepted`() {
        val imageId = UUID.randomUUID()
        every { updateProfilePhotoUseCase.updatePhoto(any()) } returns UpdateProfilePhotoResult.Accepted(imageId)

        mockMvc
            .perform(multipart("/api/v1/users/me/photo").file(photoFile()))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.imageId").value(imageId.toString()))
            .andExpect(jsonPath("$.status").value("PROCESSING"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 415 when the profile photo type is not supported`() {
        every { updateProfilePhotoUseCase.updatePhoto(any()) } returns UpdateProfilePhotoResult.InvalidImageType

        mockMvc
            .perform(multipart("/api/v1/users/me/photo").file(photoFile("text/plain")))
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("INVALID_IMAGE_TYPE"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 413 when the profile photo is too large`() {
        every { updateProfilePhotoUseCase.updatePhoto(any()) } returns UpdateProfilePhotoResult.ImageTooLarge

        mockMvc
            .perform(multipart("/api/v1/users/me/photo").file(photoFile()))
            .andExpect(status().isPayloadTooLarge)
            .andExpect(jsonPath("$.code").value("IMAGE_TOO_LARGE"))
    }

    @Test
    fun `should return 401 when uploading a profile photo unauthenticated`() {
        mockMvc
            .perform(multipart("/api/v1/users/me/photo").file(photoFile()))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the signed variant urls of a ready profile photo`() {
        every { getProfilePhotoUseCase.getPhotoUrl(any()) } returns
            GetProfilePhotoResult.Success(
                status = PhotoStatus.READY,
                thumbnailUrl = "https://signed.example/thumb",
                fullUrl = "https://signed.example/full",
            )

        mockMvc
            .perform(get("/api/v1/users/me/photo"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.variants.thumbnail").value("https://signed.example/thumb"))
            .andExpect(jsonPath("$.variants.full").value("https://signed.example/full"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with only the status while the profile photo is processing`() {
        every { getProfilePhotoUseCase.getPhotoUrl(any()) } returns
            GetProfilePhotoResult.Success(status = PhotoStatus.PROCESSING, thumbnailUrl = null, fullUrl = null)

        mockMvc
            .perform(get("/api/v1/users/me/photo"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.variants").isEmpty)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when no profile photo is set`() {
        every { getProfilePhotoUseCase.getPhotoUrl(any()) } returns GetProfilePhotoResult.NoPhoto

        mockMvc
            .perform(get("/api/v1/users/me/photo"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROFILE_PHOTO_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 204 when a profile photo is deleted`() {
        every { deleteProfilePhotoUseCase.deletePhoto(any()) } returns DeleteProfilePhotoResult.Success

        mockMvc
            .perform(delete("/api/v1/users/me/photo"))
            .andExpect(status().isNoContent)
    }
}

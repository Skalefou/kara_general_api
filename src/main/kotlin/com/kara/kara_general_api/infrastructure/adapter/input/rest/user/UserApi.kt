package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.CreateServerAccountRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.DeleteAccountRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.ProfilePhotoResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.ProfilePhotoUploadResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UpdateProfileRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserListResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Tag(name = "Utilisateur", description = "Gestion du compte utilisateur")
interface UserApi {
    @Operation(
        summary = "Supprimer son compte",
        description = "Anonymise toutes les données personnelles (RGPD Art. 17). Requiert la confirmation du mot de passe.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Compte supprimé avec succès"),
            ApiResponse(
                responseCode = "401",
                description = "Mot de passe incorrect",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/me")
    fun deleteAccount(
        @Valid @RequestBody request: DeleteAccountRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Modifier son profil",
        description =
            "Met à jour les champs fournis du profil (les autres restent inchangés). " +
                "En cas de changement d'email, l'email repasse en non vérifié et un code de vérification " +
                "est envoyé à la nouvelle adresse.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profil mis à jour avec succès",
                content = [Content(schema = Schema(implementation = UserResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Email déjà utilisé",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PatchMapping("/me")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Téléverser sa photo de profil",
        description =
            "Image privée (JPEG, PNG, WebP, AVIF ou HEIC, 5 Mo max). L'original est stocké dans le " +
                "bucket privé puis redimensionné en variantes (thumbnail, full) par un worker externe, de façon " +
                "asynchrone. La réponse est immédiate (202, statut PROCESSING) ; les URL signées par variante " +
                "apparaissent une fois la photo READY (voir GET /me/photo).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Original accepté ; génération des variantes lancée (statut PROCESSING)",
                content = [Content(schema = Schema(implementation = ProfilePhotoUploadResponse::class))],
            ),
            ApiResponse(
                responseCode = "413",
                description = "Image trop volumineuse",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "415",
                description = "Type d'image non supporté",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/me/photo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePhoto(
        @RequestPart("file") file: MultipartFile,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Obtenir l'URL de sa photo de profil",
        description = "Retourne une URL signée courte durée (15 min) vers la photo de profil.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "URL signée retournée",
                content = [Content(schema = Schema(implementation = ProfilePhotoResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Aucune photo de profil",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @GetMapping("/me/photo")
    fun getProfilePhoto(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Supprimer sa photo de profil",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Photo supprimée (ou aucune photo)"),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/me/photo")
    fun deleteProfilePhoto(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Lister tous les comptes",
        description = "Réservé aux administrateurs. Retourne une page de comptes (tous rôles confondus).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping
    fun listAccounts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<UserListResponse>

    @Operation(
        summary = "Créer un compte serveur",
        description =
            "Réservé aux administrateurs. Génère un mot de passe temporaire (32 caractères, valable 24h) " +
                "et envoie une invitation par email.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Compte serveur créé avec succès",
                content = [Content(schema = Schema(implementation = UserResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Email déjà utilisé",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun createServerAccount(
        @Valid @RequestBody request: CreateServerAccountRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Modifier un compte",
        description = "Réservé aux administrateurs. Met à jour les champs fournis d'un compte identifié par son id.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Compte mis à jour avec succès",
                content = [Content(schema = Schema(implementation = UserResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Email déjà utilisé",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PatchMapping("/{id}")
    fun updateAccount(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Réinviter un compte serveur",
        description =
            "Réservé aux administrateurs. Génère un nouveau mot de passe temporaire, invalide l'ancien " +
                "et renvoie une invitation par email.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Invitation renvoyée avec succès"),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Le compte n'est pas un compte serveur",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/{id}/reinvite")
    fun reinviteServerAccount(
        @PathVariable id: UUID,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Désactiver un compte",
        description = "Réservé aux administrateurs. Le compte ne peut plus se connecter mais reste visible.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Compte désactivé avec succès"),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Compte déjà désactivé",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/{id}/deactivate")
    fun deactivateAccount(
        @PathVariable id: UUID,
    ): ResponseEntity<Any>
}

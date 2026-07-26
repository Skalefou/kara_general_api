package com.kara.kara_general_api.infrastructure.adapter.input.rest.room

import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.CreateRoomRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomImageUploadResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomListResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.UpdateRoomRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "Salles", description = "Gestion des salles")
interface RoomApi {
    @Operation(
        summary = "Créer une salle",
        description = "Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Salle créée avec succès",
                content = [Content(schema = Schema(implementation = RoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description =
                    "Requête invalide, adresse non localisable (ADDRESS_NOT_GEOCODABLE) " +
                        "ou service inconnu (UNKNOWN_SERVICE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "Service de géolocalisation indisponible (GEOCODING_UNAVAILABLE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun createRoom(
        @Valid @RequestBody request: CreateRoomRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Lister les salles",
        description =
            "Pagination simple par défaut. Filtrage optionnel par fenêtre géographique (bbox) : " +
                "fournir les 4 paramètres minLat/minLng/maxLat/maxLng ensemble (aucune limite de taille de bbox). " +
                "En mode bbox, page/size sont ignorés et la réponse porte mode, totalInBbox, truncated et clusters. " +
                "Bascule automatique : si totalInBbox <= kara.rooms.viewport.max-results (défaut 1000) la réponse " +
                "renvoie les salles unitaires (mode \"rooms\") ; sinon elle renvoie des agrégats de clustering " +
                "serveur sur une grille N×N (mode \"clusters\", kara.rooms.viewport.cluster-grid-size, défaut 8), " +
                "la somme des count des clusters valant totalInBbox.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des salles",
                content = [Content(schema = Schema(implementation = RoomListResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Paramètres bbox incomplets (BBOX_INCOMPLETE) ou invalides (BBOX_INVALID)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @GetMapping
    fun listRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Latitude sud de la bbox (coin sud-ouest). Requiert les 3 autres bornes.")
        @RequestParam(required = false) minLat: Double?,
        @Parameter(description = "Longitude ouest de la bbox (coin sud-ouest). Requiert les 3 autres bornes.")
        @RequestParam(required = false) minLng: Double?,
        @Parameter(description = "Latitude nord de la bbox (coin nord-est). Requiert les 3 autres bornes.")
        @RequestParam(required = false) maxLat: Double?,
        @Parameter(description = "Longitude est de la bbox (coin nord-est). Requiert les 3 autres bornes.")
        @RequestParam(required = false) maxLng: Double?,
    ): ResponseEntity<Any>

    @Operation(summary = "Consulter une salle")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Salle trouvée",
                content = [Content(schema = Schema(implementation = RoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @GetMapping("/{id}")
    fun getRoom(
        @PathVariable id: UUID,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Modifier une salle",
        description = "Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Salle modifiée avec succès",
                content = [Content(schema = Schema(implementation = RoomResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Adresse non localisable (ADDRESS_NOT_GEOCODABLE) ou service inconnu (UNKNOWN_SERVICE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "Service de géolocalisation indisponible (GEOCODING_UNAVAILABLE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PatchMapping("/{id}")
    fun updateRoom(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRoomRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Supprimer une salle",
        description = "Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Salle supprimée avec succès"),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteRoom(
        @PathVariable id: UUID,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Ajouter une image à une salle",
        description =
            "Réservé aux administrateurs. L'original (JPEG, PNG, WebP, AVIF ou HEIC, 5 Mo max) est " +
                "stocké dans le bucket privé puis redimensionné en variantes WebP par un worker externe, de façon " +
                "asynchrone. La réponse est immédiate (202, statut PROCESSING) ; les variantes publiques (CDN) " +
                "apparaissent une fois l'image READY (voir GET de la salle).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Original accepté ; traitement des variantes lancé (statut PROCESSING)",
                content = [Content(schema = Schema(implementation = RoomImageUploadResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
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
    @PostMapping("/{id}/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addRoomImage(
        @PathVariable id: UUID,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Supprimer une image d'une salle",
        description = "Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Image supprimée avec succès"),
            ApiResponse(
                responseCode = "404",
                description = "Salle ou image introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}/images/{imageId}")
    fun removeRoomImage(
        @PathVariable id: UUID,
        @PathVariable imageId: UUID,
    ): ResponseEntity<Any>
}

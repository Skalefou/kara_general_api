package com.kara.kara_general_api.infrastructure.adapter.input.rest.favorite

import com.kara.kara_general_api.infrastructure.adapter.input.rest.favorite.dto.FavoriteRoomIdsResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "Lieux favoris", description = "Salles mises en favori par l'utilisateur connecté")
interface FavoriteApi {
    @Operation(
        summary = "Lister mes lieux favoris",
        description = "Salles favorites de l'utilisateur connecté, du favori le plus récent au plus ancien.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste paginée des salles favorites",
                content = [Content(schema = Schema(implementation = RoomListResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Authentification requise"),
        ],
    )
    @GetMapping
    fun listFavorites(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Lister les identifiants de mes lieux favoris",
        description =
            "Ensemble complet, non paginé, des identifiants de salles favorites. Destiné à l'affichage de " +
                "l'état « favori » sur les listes et fiches de salles sans recharger les salles elles-mêmes.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Identifiants des salles favorites",
                content = [Content(schema = Schema(implementation = FavoriteRoomIdsResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Authentification requise"),
        ],
    )
    @GetMapping("/ids")
    fun listFavoriteRoomIds(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Ajouter une salle à mes favoris",
        description = "Idempotent : ajouter une salle déjà favorite renvoie 204 sans créer de doublon.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Salle ajoutée aux favoris"),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(responseCode = "401", description = "Authentification requise"),
        ],
    )
    @PutMapping("/{roomId}")
    fun addFavorite(
        @PathVariable roomId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Retirer une salle de mes favoris",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Salle retirée des favoris"),
            ApiResponse(
                responseCode = "404",
                description = "Cette salle ne fait pas partie des favoris (FAVORITE_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(responseCode = "401", description = "Authentification requise"),
        ],
    )
    @DeleteMapping("/{roomId}")
    fun removeFavorite(
        @PathVariable roomId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.stock

import com.kara.kara_general_api.infrastructure.adapter.input.rest.stock.dto.RoomStockItemResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.stock.dto.SetRoomStockRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Stock des salles", description = "Gestion du stock de produits par salle (serveur de service et admin)")
interface RoomStockApi {
    @Operation(
        summary = "Consulter le stock d'une salle",
        description =
            "Liste les produits en stock d'une salle avec leur quantité. Accessible à l'administrateur " +
                "et au serveur de service dans cette salle (créneau couvrant l'instant présent).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Stock de la salle",
                content = [Content(array = ArraySchema(schema = Schema(implementation = RoomStockItemResponse::class)))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Serveur non de service dans cette salle (NOT_AUTHORIZED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @GetMapping
    fun getRoomStock(
        @PathVariable roomId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Définir la quantité d'un produit dans une salle",
        description =
            "Ajoute le produit au stock de la salle ou met à jour sa quantité (upsert). Accessible à " +
                "l'administrateur et au serveur de service dans cette salle.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Stock mis à jour",
                content = [Content(schema = Schema(implementation = RoomStockItemResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide (VALIDATION_ERROR)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Serveur non de service dans cette salle (NOT_AUTHORIZED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle (ROOM_NOT_FOUND) ou produit (PRODUCT_NOT_FOUND) introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PutMapping("/{productId}")
    fun setRoomStock(
        @PathVariable roomId: UUID,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: SetRoomStockRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Retirer un produit du stock d'une salle",
        description =
            "Retire un produit du stock de la salle. Accessible à l'administrateur et au serveur de " +
                "service dans cette salle.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Produit retiré du stock"),
            ApiResponse(
                responseCode = "403",
                description = "Serveur non de service dans cette salle (NOT_AUTHORIZED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle (ROOM_NOT_FOUND) ou produit absent du stock (STOCK_ITEM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/{productId}")
    fun removeRoomStock(
        @PathVariable roomId: UUID,
        @PathVariable productId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

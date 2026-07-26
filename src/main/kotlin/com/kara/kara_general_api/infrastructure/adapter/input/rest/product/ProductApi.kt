package com.kara.kara_general_api.infrastructure.adapter.input.rest.product

import com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto.CreateProductRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto.ProductResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.product.dto.UpdateProductRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Produits", description = "Catalogue générique des produits consommables (back-office)")
interface ProductApi {
    @Operation(
        summary = "Créer un produit",
        description = "Ajoute un produit au catalogue générique. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Produit créé avec succès",
                content = [Content(schema = Schema(implementation = ProductResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide (VALIDATION_ERROR)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun createProduct(
        @Valid @RequestBody request: CreateProductRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Lister les produits",
        description = "Catalogue générique complet, ordonné par nom. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Catalogue des produits",
                content = [
                    Content(
                        array =
                            io.swagger.v3.oas.annotations.media
                                .ArraySchema(schema = Schema(implementation = ProductResponse::class)),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun listProducts(): ResponseEntity<Any>

    @Operation(
        summary = "Modifier un produit",
        description = "Mise à jour partielle du produit (champs omis inchangés). Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Produit modifié avec succès",
                content = [Content(schema = Schema(implementation = ProductResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide (VALIDATION_ERROR)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Produit introuvable (PRODUCT_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PatchMapping("/{id}")
    fun updateProduct(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProductRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Supprimer un produit",
        description = "Retire un produit du catalogue générique. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Produit supprimé avec succès"),
            ApiResponse(
                responseCode = "404",
                description = "Produit introuvable (PRODUCT_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteProduct(
        @PathVariable id: UUID,
    ): ResponseEntity<Any>
}

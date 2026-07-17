package com.kara.kara_general_api.infrastructure.adapter.input.rest.service

import com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto.CreateServiceRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto.ServiceResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto.UpdateServiceRequest
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

@Tag(name = "Services", description = "Catalogue global des services réutilisables (back-office)")
interface ServiceApi {

    @Operation(
        summary = "Créer un service",
        description = "Ajoute un service au catalogue global. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Service créé avec succès",
                content = [Content(schema = Schema(implementation = ServiceResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide (VALIDATION_ERROR)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun createService(@Valid @RequestBody request: CreateServiceRequest): ResponseEntity<Any>

    @Operation(
        summary = "Lister les services",
        description = "Catalogue global complet, ordonné par libellé. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Catalogue des services",
                content = [Content(array = io.swagger.v3.oas.annotations.media.ArraySchema(schema = Schema(implementation = ServiceResponse::class)))],
            ),
        ],
    )
    @GetMapping
    fun listServices(): ResponseEntity<Any>

    @Operation(
        summary = "Modifier un service",
        description = "Mise à jour partielle du service (champs omis inchangés). Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Service modifié avec succès",
                content = [Content(schema = Schema(implementation = ServiceResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide (VALIDATION_ERROR)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Service introuvable (SERVICE_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PatchMapping("/{id}")
    fun updateService(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateServiceRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Supprimer un service",
        description = "Retire un service du catalogue global. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Service supprimé avec succès"),
            ApiResponse(
                responseCode = "404",
                description = "Service introuvable (SERVICE_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteService(@PathVariable id: UUID): ResponseEntity<Any>
}

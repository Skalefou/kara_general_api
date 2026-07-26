package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingExtensionResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CreateExtensionRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.ExtensionOptionsResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.payment.dto.InitiateBookingPaymentResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.CreateExtensionPoolRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolResponse
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
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Extensions de réservation", description = "Prolongation payante d'une réservation en cours")
interface BookingExtensionApi {
    @Operation(
        summary = "Durées de prolongation disponibles",
        description =
            "Retourne la durée maximale de prolongation possible (aucune réservation suivante ni " +
                "fermeture de la salle sur le créneau) et le prix des paliers proposés. Réservé au client " +
                "propriétaire de la réservation, pendant celle-ci.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Durées et tarifs disponibles",
                content = [Content(schema = Schema(implementation = ExtensionOptionsResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "La réservation n'appartient pas à l'utilisateur (NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation ou salle introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Réservation non confirmée, terminée, ou extension déjà en attente",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    fun getExtensionOptions(
        @PathVariable bookingId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Demander une prolongation",
        description =
            "Crée une extension PENDING pour la durée demandée. Le créneau n'est appliqué à la " +
                "réservation qu'une fois l'extension réglée : PAY_ALL via un PaymentIntent Stripe, SHARED_POT via " +
                "une cagnotte dédiée dont la capture n'intervient qu'à complétude.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Extension créée, en attente de règlement",
                content = [Content(schema = Schema(implementation = BookingExtensionResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Créneau indisponible (EXTENSION_SLOT_UNAVAILABLE) ou fenêtre trop courte",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    fun createExtension(
        @PathVariable bookingId: UUID,
        @Valid @RequestBody request: CreateExtensionRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Régler une prolongation en payant tout",
        description =
            "Initie le PaymentIntent Stripe de l'extension. La confirmation effective est faite par " +
                "le webhook Stripe, qui repousse alors la fin de la réservation.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Secrets du PaymentSheet Stripe",
                content = [Content(schema = Schema(implementation = InitiateBookingPaymentResponse::class))],
            ),
        ],
    )
    fun payExtension(
        @PathVariable extensionId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Ouvrir une cagnotte pour une prolongation",
        description =
            "Crée la cagnotte de l'extension. Aucun prélèvement tant que toutes les parts ne sont " +
                "pas autorisées ; à complétude, les autorisations sont capturées et la réservation est prolongée.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Cagnotte d'extension créée",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
        ],
    )
    fun createExtensionPool(
        @PathVariable extensionId: UUID,
        @Valid @RequestBody request: CreateExtensionPoolRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

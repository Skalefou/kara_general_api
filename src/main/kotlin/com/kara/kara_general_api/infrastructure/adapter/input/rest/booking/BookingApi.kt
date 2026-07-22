package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingDetailResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CancelBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CreateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingResponse
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Réservations", description = "Réservations et estimations tarifaires")
interface BookingApi {

    @Operation(
        summary = "Estimer le prix d'une réservation",
        description = "Calcul en lecture seule, sans aucune persistance. Accessible aux invités et aux clients. " +
            "Le prix de base vaut prix/personne/heure × nombre de personnes × nombre d'heures ; " +
            "les options sont des forfaits fixes qui s'ajoutent au total.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Estimation calculée",
                content = [Content(schema = Schema(implementation = EstimateBookingResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide : moins de 2 personnes (TOO_FEW_PEOPLE), capacité dépassée " +
                    "(CAPACITY_EXCEEDED), créneau invalide (INVALID_TIME_SLOT) ou option étrangère à la salle " +
                    "(UNKNOWN_ROOM_OPTION)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/estimate")
    fun estimate(@Valid @RequestBody request: EstimateBookingRequest): ResponseEntity<Any>

    @Operation(
        summary = "Créer une réservation",
        description = "Crée une réservation persistée en statut PENDING pour le client authentifié. " +
            "Le prix total est figé par le même calcul que l'estimation. Le créneau est rejeté (409) " +
            "s'il chevauche une réservation existante (PENDING ou CONFIRMED) sur la même salle.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Réservation créée (PENDING)",
                content = [Content(schema = Schema(implementation = BookingResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Requête invalide : moins de 2 personnes (TOO_FEW_PEOPLE), capacité dépassée " +
                    "(CAPACITY_EXCEEDED), créneau invalide (INVALID_TIME_SLOT) ou option étrangère à la salle " +
                    "(UNKNOWN_ROOM_OPTION)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Créneau déjà réservé (BOOKING_SLOT_UNAVAILABLE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Détail d'une réservation (+ billet)",
        description = "Détail complet d'une réservation pour son propriétaire, incluant le code de billet " +
            "(ticketCode) rendu en QR par le front. ticketCode est non-null uniquement lorsque la réservation " +
            "est CONFIRMED.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Détail de la réservation",
                content = [Content(schema = Schema(implementation = BookingDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "La réservation n'appartient pas à l'utilisateur (BOOKING_NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation introuvable (BOOKING_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @GetMapping("/{bookingId}")
    fun getBooking(
        @PathVariable bookingId: java.util.UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Annuler une réservation",
        description = "Annule une réservation appartenant au client. Selon l'état : PENDING payer-tout → aucune " +
            "capture à libérer ; cagnotte ouverte → levée de toutes les autorisations Stripe (zéro prélèvement) ; " +
            "CONFIRMED → remboursement Stripe intégral. Réponse : résumé avec le statut CANCELLED et l'indicateur " +
            "refunded.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Réservation annulée",
                content = [Content(schema = Schema(implementation = CancelBookingResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "La réservation n'appartient pas à l'utilisateur (BOOKING_NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation introuvable (BOOKING_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Réservation déjà annulée (BOOKING_ALREADY_CANCELLED) ou déjà commencée " +
                    "(BOOKING_ALREADY_STARTED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/{bookingId}/cancel")
    fun cancelBooking(
        @PathVariable bookingId: java.util.UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

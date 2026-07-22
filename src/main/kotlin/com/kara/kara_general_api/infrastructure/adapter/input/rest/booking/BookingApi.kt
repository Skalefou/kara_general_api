package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.AdminBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingConversationResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CreateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.ServerBookingResponse
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

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
        summary = "Consulter mes réservations (serveur)",
        description = "Retourne les réservations (hors annulées) dont la salle et le créneau chevauchent " +
            "l'agenda du serveur authentifié. Récap essentiel, sans données personnelles du client. " +
            "Réservé au rôle SERVER.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Réservations rattachées au serveur",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ServerBookingResponse::class)))],
            ),
        ],
    )
    @GetMapping("/me/assigned")
    fun listMyAssignedBookings(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Consulter toutes les réservations (admin)",
        description = "Retourne toutes les réservations de la plateforme, enrichies du nom de la salle et " +
            "du nom du client, ordonnées par date décroissante. Réservé au rôle ADMIN.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Toutes les réservations",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AdminBookingResponse::class)))],
            ),
        ],
    )
    @GetMapping
    fun listAllBookings(): ResponseEntity<Any>

    @Operation(
        summary = "Ouvrir le chat d'une réservation",
        description = "Crée ou retourne la conversation rattachée à la réservation. Accessible au client de " +
            "la réservation, aux serveurs qui y sont rattachés (via leur agenda) et aux administrateurs. " +
            "La réponse indique la date de fermeture du chat (fin de la réservation + 30 min).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Conversation prête",
                content = [Content(schema = Schema(implementation = BookingConversationResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Appelant non autorisé pour cette réservation (NOT_AUTHORIZED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation introuvable (BOOKING_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/{id}/conversation")
    fun openBookingConversation(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Déclencher une alerte d'urgence",
        description = "Signale une urgence sur une réservation : diffuse une alerte temps réel (STOMP) à " +
            "chaque serveur rattaché à la réservation. Accessible au client de la réservation et aux admins.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Alerte diffusée"),
            ApiResponse(
                responseCode = "403",
                description = "Appelant non autorisé (NOT_AUTHORIZED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation introuvable (BOOKING_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/{id}/emergency")
    fun triggerEmergency(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

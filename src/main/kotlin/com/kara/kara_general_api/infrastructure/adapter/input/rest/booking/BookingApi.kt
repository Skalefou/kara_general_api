package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.AdminBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingConversationResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingDetailResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CancelBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CreateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.ServerBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.UserBookingResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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
        description =
            "Calcul en lecture seule, sans aucune persistance. Accessible aux invités et aux clients. " +
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
                description =
                    "Requête invalide : moins de 2 personnes (TOO_FEW_PEOPLE), capacité dépassée " +
                        "(CAPACITY_EXCEEDED), créneau invalide (INVALID_TIME_SLOT), durée inférieure à une heure " +
                        "(BOOKING_DURATION_TOO_SHORT) ou option étrangère à la salle (UNKNOWN_ROOM_OPTION)",
                content = [
                    Content(
                        schema = Schema(implementation = ProblemDetail::class),
                        examples = [
                            ExampleObject(
                                name = "BOOKING_DURATION_TOO_SHORT",
                                value = """
                                    {
                                      "title": "Durée trop courte",
                                      "status": 400,
                                      "detail": "Une réservation doit durer au minimum 60 minutes.",
                                      "code": "BOOKING_DURATION_TOO_SHORT",
                                      "minimumMinutes": 60
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/estimate")
    fun estimate(
        @Valid @RequestBody request: EstimateBookingRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Créer une réservation",
        description =
            "Crée une réservation persistée en statut PENDING pour le client authentifié. " +
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
                description =
                    "Requête invalide : moins de 2 personnes (TOO_FEW_PEOPLE), capacité dépassée " +
                        "(CAPACITY_EXCEEDED), créneau invalide (INVALID_TIME_SLOT), durée inférieure à une heure " +
                        "(BOOKING_DURATION_TOO_SHORT) ou option étrangère à la salle (UNKNOWN_ROOM_OPTION)",
                content = [
                    Content(
                        schema = Schema(implementation = ProblemDetail::class),
                        examples = [
                            ExampleObject(
                                name = "BOOKING_DURATION_TOO_SHORT",
                                value = """
                                    {
                                      "title": "Durée trop courte",
                                      "status": 400,
                                      "detail": "Une réservation doit durer au minimum 60 minutes.",
                                      "code": "BOOKING_DURATION_TOO_SHORT",
                                      "minimumMinutes": 60
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
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
        description =
            "Retourne les réservations (hors annulées) dont la salle et le créneau chevauchent " +
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
        summary = "Consulter mes réservations (client)",
        description =
            "Retourne toutes les réservations dont l'utilisateur authentifié est le propriétaire, " +
                "quel que soit son rôle. **Aucun statut n'est filtré** (PENDING, CONFIRMED, CANCELLED) : " +
                "c'est le front qui étiquette et regroupe. Chaque réservation porte " +
                "l'horaire, le nombre de personnes, les services retenus et, en mode SHARED_POT uniquement, la " +
                "cagnotte incluse en ligne avec la liste nominative des parts — le front n'a donc pas à " +
                "interroger une cagnotte par réservation. En mode PAY_ALL, `pool` vaut null : le modèle ne " +
                "connaît aucun nom de participant, seulement `numberOfPeople`. Tri par date de début " +
                "décroissante.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Réservations de l'utilisateur (liste éventuellement vide)",
                content = [
                    Content(
                        array = ArraySchema(schema = Schema(implementation = UserBookingResponse::class)),
                        examples = [
                            ExampleObject(
                                name = "Une cagnotte et une réservation payée en solo",
                                value = """
                                    [
                                      {
                                        "bookingId": "9f1c4f0e-1f4a-4c2b-9a3d-1b2c3d4e5f60",
                                        "roomId": "550e8400-e29b-41d4-a716-446655440000",
                                        "roomName": "Salle Étoile",
                                        "roomAddress": "12 rue de Paris, 69002 Lyon, France",
                                        "startAt": "2026-08-01T18:00:00Z",
                                        "endAt": "2026-08-01T21:00:00Z",
                                        "status": "PENDING",
                                        "paymentMode": "SHARED_POT",
                                        "numberOfPeople": 8,
                                        "totalPrice": 435.00,
                                        "currency": "EUR",
                                        "expiresAt": "2026-08-01T16:00:00Z",
                                        "options": [
                                          {
                                            "optionId": "c0ffee00-0000-4000-8000-000000000001",
                                            "label": "Ménage fin de soirée",
                                            "price": 60.00,
                                            "currency": "EUR"
                                          }
                                        ],
                                        "pool": {
                                          "poolId": "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                                          "status": "OPEN",
                                          "targetAmount": 435.00,
                                          "collectedAmount": 217.50,
                                          "currency": "EUR",
                                          "percentage": 50,
                                          "deadline": "2026-08-01T16:00:00Z",
                                          "shares": [
                                            {
                                              "shareId": "11111111-1111-4111-8111-111111111111",
                                              "participantName": "Jeanne Martin",
                                              "email": "jeanne@example.com",
                                              "amount": 217.50,
                                              "status": "AUTHORIZED"
                                            },
                                            {
                                              "shareId": "22222222-2222-4222-8222-222222222222",
                                              "participantName": "Karim Belkacem",
                                              "email": null,
                                              "amount": 217.50,
                                              "status": "PENDING"
                                            }
                                          ]
                                        }
                                      },
                                      {
                                        "bookingId": "7d2b8a10-3c4d-4e5f-9a0b-1c2d3e4f5a6b",
                                        "roomId": "550e8400-e29b-41d4-a716-446655440000",
                                        "roomName": "Salle Étoile",
                                        "roomAddress": "12 rue de Paris, 69002 Lyon, France",
                                        "startAt": "2026-07-04T19:00:00Z",
                                        "endAt": "2026-07-04T23:00:00Z",
                                        "status": "CONFIRMED",
                                        "paymentMode": "PAY_ALL",
                                        "numberOfPeople": 4,
                                        "totalPrice": 280.00,
                                        "currency": "EUR",
                                        "expiresAt": "2026-07-01T10:15:00Z",
                                        "options": [],
                                        "pool": null
                                      }
                                    ]
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", description = "Requête non authentifiée"),
        ],
    )
    @GetMapping("/me")
    fun listMyBookings(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Consulter toutes les réservations (admin)",
        description =
            "Retourne toutes les réservations de la plateforme, enrichies du nom de la salle et " +
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
        summary = "Détail d'une réservation (+ billet)",
        description =
            "Détail complet d'une réservation pour son propriétaire, incluant le code de billet " +
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
        summary = "Ouvrir le chat d'une réservation",
        description =
            "Crée ou retourne la conversation rattachée à la réservation. Accessible au client de " +
                "la réservation, aux serveurs qui y sont rattachés (via leur agenda) et aux administrateurs. " +
                "La réponse indique la date de fermeture du chat (fin de la réservation + 24 h).",
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
        description =
            "Signale une urgence sur une réservation : diffuse une alerte temps réel (STOMP) à " +
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

    @Operation(
        summary = "Annuler une réservation",
        description =
            "Annule une réservation appartenant au client. Selon l'état : PENDING payer-tout → aucune " +
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
                description =
                    "Réservation déjà annulée (BOOKING_ALREADY_CANCELLED) ou déjà commencée " +
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

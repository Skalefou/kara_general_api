package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingAccessResponse
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
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "Contrôle d'accès", description = "Validation du billet d'une réservation en face à face")
interface BookingAccessApi {

    @Operation(
        summary = "Valider le billet d'un client",
        description = "Le serveur scanne le QR du billet, qui encode l'identifiant de la réservation. " +
            "L'accès est accordé si la réservation est confirmée, si le contrôleur est rattaché à la salle " +
            "sur ce créneau (un administrateur contrôle partout) et si l'on se trouve dans la fenêtre " +
            "d'admission : 30 minutes avant le début, jusqu'à la fin. L'entrée n'est tracée qu'une fois ; " +
            "une seconde présentation du même billet renvoie 409 avec l'horodatage de la première.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Accès accordé, entrée enregistrée",
                content = [Content(schema = Schema(implementation = BookingAccessResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Le contrôleur n'est pas rattaché à la salle sur ce créneau (NOT_ASSIGNED_SERVER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Réservation ou salle introuvable",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Réservation non confirmée (BOOKING_NOT_CONFIRMED), hors fenêtre d'admission " +
                    "(OUTSIDE_ADMISSION_WINDOW) ou billet déjà validé (BOOKING_ALREADY_CHECKED_IN). Le " +
                    "récapitulatif de la réservation est joint dans la propriété « booking ».",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    fun validateAccess(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

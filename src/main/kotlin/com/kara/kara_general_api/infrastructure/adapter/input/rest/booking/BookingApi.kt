package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
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
}

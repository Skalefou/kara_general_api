package com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift

import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.CreateServerShiftRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.ServerShiftResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.ServerShiftWithRoomResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.UpdateServerShiftRequest
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant
import java.util.UUID

@Tag(
    name = "Agenda serveurs",
    description = "Édition de l'agenda des serveurs : affectation à une salle sur un créneau (back-office, ADMIN)",
)
interface ServerShiftApi {
    @Operation(
        summary = "Lister les créneaux d'agenda",
        description =
            "Retourne les créneaux de tous les serveurs, ordonnés par date de début. " +
                "Filtres optionnels : serveur, salle et fenêtre temporelle. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des créneaux",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ServerShiftResponse::class)))],
            ),
        ],
    )
    @GetMapping
    fun listServerShifts(
        @RequestParam(required = false) serverId: UUID?,
        @RequestParam(required = false) roomId: UUID?,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Consulter mon agenda (serveur)",
        description =
            "Retourne les créneaux du serveur authentifié, enrichis du nom et de la ville de la " +
                "salle où il doit se rendre, ordonnés par date. Réservé au rôle SERVER.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Créneaux du serveur",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ServerShiftWithRoomResponse::class)))],
            ),
        ],
    )
    @GetMapping("/me")
    fun listMyShifts(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Créer un créneau d'agenda",
        description =
            "Affecte un serveur à une salle sur un créneau. Le serveur doit exister et avoir le " +
                "rôle SERVER. Le créneau est rejeté (409) s'il chevauche un autre créneau du même serveur. " +
                "Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Créneau créé",
                content = [Content(schema = Schema(implementation = ServerShiftResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Créneau invalide (INVALID_TIME_SLOT) ou compte cible non serveur (NOT_A_SERVER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Serveur introuvable (SERVER_NOT_FOUND) ou salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Créneau en conflit avec un autre créneau du serveur (SHIFT_SLOT_UNAVAILABLE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping
    fun createServerShift(
        @Valid @RequestBody request: CreateServerShiftRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Modifier un créneau d'agenda",
        description = "Met à jour la salle, les bornes horaires ou la note d'un créneau. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Créneau mis à jour",
                content = [Content(schema = Schema(implementation = ServerShiftResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Créneau invalide (INVALID_TIME_SLOT)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Créneau introuvable (SHIFT_NOT_FOUND) ou salle introuvable (ROOM_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Créneau en conflit avec un autre créneau du serveur (SHIFT_SLOT_UNAVAILABLE)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PatchMapping("/{id}")
    fun updateServerShift(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateServerShiftRequest,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Supprimer un créneau d'agenda",
        description = "Retire un créneau de l'agenda. Réservé aux administrateurs.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Créneau supprimé"),
            ApiResponse(
                responseCode = "404",
                description = "Créneau introuvable (SHIFT_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteServerShift(
        @PathVariable id: UUID,
    ): ResponseEntity<Any>
}

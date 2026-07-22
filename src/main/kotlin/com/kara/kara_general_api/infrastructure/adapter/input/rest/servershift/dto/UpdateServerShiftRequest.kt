package com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/**
 * Mise à jour partielle d'un créneau : chaque champ non-null remplace la valeur existante. Pour vider la
 * note, envoyer [clearNote] = true (un `note` null seul laisse la note inchangée).
 */
data class UpdateServerShiftRequest(
    @field:Schema(description = "Nouvelle salle (inchangée si absente)", nullable = true)
    val roomId: UUID? = null,
    @field:Schema(description = "Nouveau début de créneau (inchangé si absent)", nullable = true)
    val startAt: Instant? = null,
    @field:Schema(description = "Nouvelle fin de créneau (inchangée si absente)", nullable = true)
    val endAt: Instant? = null,
    @field:Size(max = 500)
    @field:Schema(description = "Nouvelle note (inchangée si absente)", nullable = true)
    val note: String? = null,
    @field:Schema(description = "Force la suppression de la note existante", example = "false")
    val clearNote: Boolean = false,
)

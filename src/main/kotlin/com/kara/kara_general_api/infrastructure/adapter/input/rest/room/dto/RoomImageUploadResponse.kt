package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/** Réponse 202 de l'ajout d'image de salle : traitement des variantes lancé de façon asynchrone. */
data class RoomImageUploadResponse(
    @field:Schema(description = "Identifiant de l'image créée (à suivre jusqu'au statut READY)")
    val imageId: UUID,
    @field:Schema(description = "Statut initial", example = "PROCESSING", allowableValues = ["PROCESSING"])
    val status: String = "PROCESSING",
)

package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/** Réponse 202 du téléversement de photo de profil : génération des variantes lancée de façon asynchrone. */
data class ProfilePhotoUploadResponse(
    @field:Schema(description = "Identifiant de l'image en cours de traitement")
    val imageId: UUID,
    @field:Schema(description = "Statut initial", example = "PROCESSING", allowableValues = ["PROCESSING"])
    val status: String = "PROCESSING",
)

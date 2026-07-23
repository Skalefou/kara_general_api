package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Représentation de la photo de profil en lecture. Les URL signées par variante (thumbnail/full) ne sont
 * présentes que lorsque [status] == READY ; sinon seul le statut est renseigné.
 */
data class ProfilePhotoResponse(
    @field:Schema(description = "Statut de traitement", allowableValues = ["PROCESSING", "READY", "FAILED"])
    val status: String,
    @field:Schema(
        description = "URL signées courte durée par variante (thumbnail/full). Présent uniquement si READY.",
        nullable = true,
    )
    val variants: Map<String, String>? = null,
)

package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateRoomRequest(
    @field:Schema(description = "Nom de la salle", example = "Salle Étoile")
    val name: String? = null,
    @field:Schema(description = "Adresse de la salle", example = "12 rue de la Paix")
    val street: String? = null,
    @field:Schema(description = "Ville", example = "Paris")
    val city: String? = null,
    @field:Schema(description = "Code postal", example = "75002")
    val postalCode: String? = null,
    @field:Schema(description = "Pays", example = "France")
    val country: String? = null,
)

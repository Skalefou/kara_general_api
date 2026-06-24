package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class RegisterResponse(
    @field:Schema(description = "Identifiant unique de l'utilisateur créé")
    val id: UUID,
    @field:Schema(description = "Adresse email de l'utilisateur", example = "jane.doe@example.com")
    val email: String,
    @field:Schema(description = "Prénom de l'utilisateur", example = "Jane")
    val firstName: String,
    @field:Schema(description = "Nom de famille de l'utilisateur", example = "Doe")
    val lastName: String,
)

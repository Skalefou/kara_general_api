package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class LoginRequest(
    @field:NotBlank
    @field:Schema(description = "Email ou numéro de téléphone de l'utilisateur", example = "jane.doe@example.com")
    val identifiant: String,
    @field:NotBlank
    @field:Schema(description = "Mot de passe en clair", example = "S3cur3P@ssw0rd")
    val password: String,
    @field:NotNull
    @get:JsonProperty("isEmail")
    @field:Schema(description = "Indique si l'identifiant est un email (true) ou un numéro de téléphone (false)", example = "true")
    val isEmail: Boolean,
)

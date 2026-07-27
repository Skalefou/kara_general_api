package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TwoFactorSetupRequest(
    @field:NotBlank
    @field:Schema(description = "Mot de passe actuel, pour confirmer l'identité de l'utilisateur", example = "S3cur3P@ssw0rd")
    val password: String,
)

package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TwoFactorLoginRequest(
    @field:NotBlank
    @field:Schema(description = "Jeton de challenge délivré par POST /api/v1/auth/login")
    val mfaToken: String,
    @field:NotBlank
    @field:Schema(description = "Code à usage unique affiché par l'application d'authentification", example = "123456")
    val code: String,
)

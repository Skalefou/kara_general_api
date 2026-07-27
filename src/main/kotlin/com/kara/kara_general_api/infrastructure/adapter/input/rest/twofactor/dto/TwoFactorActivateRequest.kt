package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TwoFactorActivateRequest(
    @field:NotBlank
    @field:Schema(description = "Premier code à usage unique affiché par l'application d'authentification", example = "123456")
    val code: String,
)

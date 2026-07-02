package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank
    @field:Schema(description = "Refresh token à échanger contre un nouveau couple de tokens")
    val refreshToken: String,
)

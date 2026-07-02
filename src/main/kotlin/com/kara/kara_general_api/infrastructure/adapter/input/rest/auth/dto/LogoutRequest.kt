package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class LogoutRequest(
    @field:NotBlank
    @field:Schema(description = "Refresh token à révoquer")
    val refreshToken: String,
)

package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

data class RefreshTokenResponse(
    @field:Schema(description = "Nouvel access token JWT (RS256)")
    val accessToken: String,
    @field:Schema(description = "Durée de validité de l'access token, en secondes", example = "900")
    val expiresIn: Long,
    @field:Schema(description = "Nouveau refresh token (rotation : l'ancien est invalidé)")
    val refreshToken: String,
    @field:Schema(description = "Durée de validité du refresh token, en secondes", example = "604800")
    val refreshTokenExpiresIn: Long,
)

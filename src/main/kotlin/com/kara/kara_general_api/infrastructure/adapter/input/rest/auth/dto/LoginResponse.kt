package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.UserResponse
import io.swagger.v3.oas.annotations.media.Schema

data class LoginResponse(
    @field:Schema(description = "Access token JWT (RS256)")
    val accessToken: String,
    @field:Schema(description = "Durée de validité du token, en secondes", example = "900")
    val expiresIn: Long,
    @field:Schema(description = "Refresh token opaque à conserver pour renouveler l'access token")
    val refreshToken: String,
    @field:Schema(description = "Durée de validité du refresh token, en secondes", example = "604800")
    val refreshTokenExpiresIn: Long,
    @field:Schema(description = "Profil de l'utilisateur connecté")
    val user: UserResponse,
    @field:Schema(
        description = "Indique que l'utilisateur doit changer son mot de passe temporaire avant toute autre action",
        example = "false",
    )
    val mustChangePassword: Boolean,
)

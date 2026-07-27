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
    @field:Schema(
        description =
            "Toujours `false` sur ce schéma. Champ commun avec `TwoFactorChallengeResponse` : le front " +
                "discrimine les deux formes du 200 de /api/v1/auth/login sur ce seul champ.",
        example = "false",
    )
    val twoFactorRequired: Boolean = false,
    @field:Schema(
        description =
            "Vrai uniquement sur /api/v1/auth/login/2fa/recovery : la consommation du code de secours a " +
                "désactivé l'A2F du compte, qui devra être reconfigurée.",
        example = "false",
    )
    val twoFactorDisabled: Boolean = false,
)

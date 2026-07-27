package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description =
        "Réponse alternative de POST /api/v1/auth/login : le mot de passe est correct mais le compte exige " +
            "un second facteur. Aucun token n'est délivré ; le front discrimine sur `twoFactorRequired`.",
)
data class TwoFactorChallengeResponse(
    @field:Schema(description = "Toujours `true` sur ce schéma", example = "true")
    val twoFactorRequired: Boolean = true,
    @field:Schema(description = "Jeton opaque à rejouer sur /api/v1/auth/login/2fa ou /api/v1/auth/login/2fa/recovery")
    val mfaToken: String,
    @field:Schema(description = "Durée de validité du challenge, en secondes", example = "300")
    val expiresIn: Long,
)

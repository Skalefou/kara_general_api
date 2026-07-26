package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RecoveryCodeLoginRequest(
    @field:NotBlank
    @field:Schema(description = "Jeton de challenge délivré par POST /api/v1/auth/login")
    val mfaToken: String,
    @field:NotBlank
    @field:Schema(
        description =
            "Code de secours à usage unique (mots séparés par des espaces). Sa consommation désactive " +
                "l'A2F du compte.",
        example = "cascade tulipe marteau renard",
    )
    val recoveryCode: String,
)

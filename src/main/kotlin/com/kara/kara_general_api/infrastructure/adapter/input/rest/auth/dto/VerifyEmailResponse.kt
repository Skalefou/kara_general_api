package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

data class VerifyEmailResponse(
    @field:Schema(description = "Access token JWT (RS256)")
    val accessToken: String,
    @field:Schema(description = "Durée de validité du token, en secondes", example = "900")
    val expiresIn: Long,
)

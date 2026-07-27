package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class DisableTwoFactorRequest(
    @field:NotBlank
    @field:Schema(description = "Mot de passe actuel", example = "S3cur3P@ssw0rd")
    val password: String,
    @field:NotBlank
    @field:Schema(
        description = "Code à usage unique courant OU code de secours non consommé",
        example = "123456",
    )
    val code: String,
)

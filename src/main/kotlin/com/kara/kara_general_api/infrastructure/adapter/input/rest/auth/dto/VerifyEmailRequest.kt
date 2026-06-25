package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class VerifyEmailRequest(
    @field:NotBlank @field:Email
    @field:Schema(description = "Adresse email de l'utilisateur", example = "jane.doe@example.com")
    val email: String,
    @field:NotBlank
    @field:Schema(description = "Code de vérification à 6 chiffres reçu par email", example = "123456")
    val code: String,
)

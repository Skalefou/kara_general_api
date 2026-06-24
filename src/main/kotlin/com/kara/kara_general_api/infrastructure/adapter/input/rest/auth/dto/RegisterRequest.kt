package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class RegisterRequest(
    @field:NotBlank @field:Email
    @field:Schema(description = "Adresse email de l'utilisateur", example = "jane.doe@example.com")
    val email: String,
    @field:NotBlank
    @field:Schema(description = "Mot de passe en clair (sera haché)", example = "S3cur3P@ssw0rd")
    val password: String,
    @field:NotBlank
    @field:Schema(description = "Prénom de l'utilisateur", example = "Jane")
    val firstName: String,
    @field:NotBlank
    @field:Schema(description = "Nom de famille de l'utilisateur", example = "Doe")
    val lastName: String,
    @field:NotBlank
    @field:Schema(description = "Numéro de téléphone de l'utilisateur", example = "+33612345678")
    val phoneNumber: String,
    @field:NotNull
    @field:Schema(description = "Date de naissance de l'utilisateur", example = "1990-01-15")
    val birthDate: LocalDate,
)

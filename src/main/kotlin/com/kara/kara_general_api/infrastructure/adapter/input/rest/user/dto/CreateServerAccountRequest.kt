package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateServerAccountRequest(
    @field:NotBlank @field:Email
    @field:Schema(description = "Adresse email du serveur", example = "server@example.com")
    val email: String,
    @field:NotBlank
    @field:Schema(description = "Prénom du serveur", example = "Jane")
    val firstName: String,
    @field:NotBlank
    @field:Schema(description = "Nom de famille du serveur", example = "Doe")
    val lastName: String,
    @field:NotBlank
    @field:Schema(description = "Numéro de téléphone du serveur", example = "+33612345678")
    val phoneNumber: String,
    @field:NotNull
    @field:Schema(description = "Date de naissance du serveur", example = "1990-01-15")
    val birthDate: LocalDate,
)

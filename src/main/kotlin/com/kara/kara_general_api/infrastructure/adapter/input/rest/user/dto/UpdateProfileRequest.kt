package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import java.time.LocalDate

data class UpdateProfileRequest(
    @field:Schema(description = "Prénom de l'utilisateur", example = "Jane")
    val firstName: String? = null,
    @field:Schema(description = "Nom de famille de l'utilisateur", example = "Doe")
    val lastName: String? = null,
    @field:Schema(description = "Numéro de téléphone de l'utilisateur", example = "+33612345678")
    val phoneNumber: String? = null,
    @field:Schema(description = "Date de naissance de l'utilisateur", example = "1990-01-15")
    val birthDate: LocalDate? = null,
    @field:Email(message = "L'email doit être valide")
    @field:Schema(description = "Adresse email de l'utilisateur", example = "jane.doe@example.com")
    val email: String? = null,
)

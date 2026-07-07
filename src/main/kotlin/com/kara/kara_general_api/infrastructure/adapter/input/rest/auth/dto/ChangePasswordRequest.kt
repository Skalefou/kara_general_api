package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest(
    @field:NotBlank
    @field:Schema(description = "Mot de passe actuel (ou temporaire)", example = "T3mp0raryP@ssw0rd...")
    val currentPassword: String,
    @field:NotBlank
    @field:Schema(description = "Nouveau mot de passe (doit respecter la politique du rôle)", example = "N3wStr0ngP@ssw0rd...")
    val newPassword: String,
)

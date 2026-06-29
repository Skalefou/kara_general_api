package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import jakarta.validation.constraints.NotBlank

data class ForgotPasswordRequest(
    @field:NotBlank val email: String,
)

package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import jakarta.validation.constraints.NotBlank

data class DeleteAccountRequest(
    @field:NotBlank val password: String,
)

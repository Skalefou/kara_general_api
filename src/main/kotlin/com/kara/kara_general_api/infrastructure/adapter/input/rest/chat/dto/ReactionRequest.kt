package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

import jakarta.validation.constraints.NotBlank

data class ReactionRequest(
    @field:NotBlank(message = "L'emoji ne peut pas être vide.")
    val emoji: String,
)

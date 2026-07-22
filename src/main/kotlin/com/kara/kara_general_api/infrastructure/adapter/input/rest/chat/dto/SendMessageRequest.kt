package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

import jakarta.validation.constraints.NotBlank

data class SendMessageRequest(
    @field:NotBlank(message = "Le message ne peut pas être vide.")
    val text: String,
    val replyToId: String? = null,
    val isForwarded: Boolean? = null,
)

package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

data class TypingRequest(
    val isTyping: Boolean = false,
)

data class TypingDto(
    val conversationId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean,
)

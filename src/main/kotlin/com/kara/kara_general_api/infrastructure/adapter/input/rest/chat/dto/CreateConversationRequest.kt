package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

data class CreateConversationRequest(
    val participantIds: List<String> = emptyList(),
)

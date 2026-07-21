package com.kara.kara_general_api.domain.model.chat

import java.time.Instant

data class Conversation(
    val id: ConversationId,
    val createdAt: Instant,
) {
    companion object {
        fun create(): Conversation = Conversation(id = ConversationId.generate(), createdAt = Instant.now())
    }
}

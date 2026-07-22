package com.kara.kara_general_api.domain.model.chat

import java.util.UUID

@JvmInline
value class ConversationId(val value: UUID) {
    companion object {
        fun generate(): ConversationId = ConversationId(UUID.randomUUID())
    }
}

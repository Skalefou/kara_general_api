package com.kara.kara_general_api.domain.model.chat

import java.util.UUID

@JvmInline
value class MessageId(
    val value: UUID,
) {
    companion object {
        fun generate(): MessageId = MessageId(UUID.randomUUID())
    }
}

package com.kara.kara_general_api.domain.model.chat

import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

const val MESSAGE_TYPE_TEXT: String = "text"

data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderId: UserId,
    val type: String,
    val text: String?,
    val replyToId: MessageId?,
    val isForwarded: Boolean,
    val isPinned: Boolean,
    val sentAt: Instant,
) {
    companion object {
        fun create(
            conversationId: ConversationId,
            senderId: UserId,
            text: String,
            replyToId: MessageId?,
            isForwarded: Boolean,
        ): Message =
            Message(
                id = MessageId.generate(),
                conversationId = conversationId,
                senderId = senderId,
                type = MESSAGE_TYPE_TEXT,
                text = text,
                replyToId = replyToId,
                isForwarded = isForwarded,
                isPinned = false,
                sentAt = Instant.now(),
            )
    }
}

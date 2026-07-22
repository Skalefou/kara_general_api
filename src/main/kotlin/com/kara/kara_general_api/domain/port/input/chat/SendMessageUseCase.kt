package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.user.UserId

data class SendMessageCommand(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val text: String,
    val replyToId: MessageId?,
    val isForwarded: Boolean,
)

sealed interface SendMessageResult {
    data class Success(val message: MessageView) : SendMessageResult

    data object ConversationNotFound : SendMessageResult

    data object NotParticipant : SendMessageResult

    data object EmptyText : SendMessageResult
}

interface SendMessageUseCase {
    fun sendMessage(command: SendMessageCommand): SendMessageResult
}

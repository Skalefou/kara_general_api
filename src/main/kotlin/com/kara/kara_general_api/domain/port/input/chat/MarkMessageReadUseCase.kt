package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.user.UserId

data class MarkMessageReadCommand(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val messageId: MessageId,
)

sealed interface MarkMessageReadResult {
    data object Success : MarkMessageReadResult

    data object ConversationNotFound : MarkMessageReadResult

    data object NotParticipant : MarkMessageReadResult

    data object MessageNotFound : MarkMessageReadResult
}

interface MarkMessageReadUseCase {
    /** Marque le message (et les précédents) comme lus par l'utilisateur courant. */
    fun markRead(command: MarkMessageReadCommand): MarkMessageReadResult
}

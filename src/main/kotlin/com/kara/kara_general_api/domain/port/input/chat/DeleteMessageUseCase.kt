package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.user.UserId

data class DeleteMessageCommand(
    val currentUserId: UserId,
    val isAdmin: Boolean,
    val conversationId: ConversationId,
    val messageId: MessageId,
)

sealed interface DeleteMessageResult {
    data object Success : DeleteMessageResult

    data object ConversationNotFound : DeleteMessageResult

    data object NotParticipant : DeleteMessageResult

    data object MessageNotFound : DeleteMessageResult

    data object NotAuthor : DeleteMessageResult
}

interface DeleteMessageUseCase {
    /** Seul l'auteur (ou un ADMIN) peut supprimer un message. */
    fun deleteMessage(command: DeleteMessageCommand): DeleteMessageResult
}

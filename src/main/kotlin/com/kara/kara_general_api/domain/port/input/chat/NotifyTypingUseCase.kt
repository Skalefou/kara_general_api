package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.user.UserId

data class NotifyTypingCommand(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val isTyping: Boolean,
)

sealed interface NotifyTypingResult {
    data object Success : NotifyTypingResult

    data object NotParticipant : NotifyTypingResult
}

interface NotifyTypingUseCase {
    /** Rediffuse l'événement de saisie aux seuls participants de la conversation. */
    fun notifyTyping(command: NotifyTypingCommand): NotifyTypingResult
}

package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationView
import com.kara.kara_general_api.domain.model.user.UserId

data class CreateConversationCommand(
    val currentUserId: UserId,
    val participantIds: Set<UserId>,
)

sealed interface CreateConversationResult {
    data class Success(
        val conversation: ConversationView,
    ) : CreateConversationResult
}

interface CreateConversationUseCase {
    /** Crée-ou-retrouve : réutilise la conversation dont l'ensemble des participants est identique. */
    fun createConversation(command: CreateConversationCommand): CreateConversationResult
}

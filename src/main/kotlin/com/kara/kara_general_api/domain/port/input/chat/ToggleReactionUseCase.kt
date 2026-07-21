package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageId
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.user.UserId

data class ToggleReactionCommand(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val messageId: MessageId,
    val emoji: String,
)

sealed interface ToggleReactionResult {
    data class Success(val message: MessageView) : ToggleReactionResult

    data object ConversationNotFound : ToggleReactionResult

    data object NotParticipant : ToggleReactionResult

    data object MessageNotFound : ToggleReactionResult
}

interface ToggleReactionUseCase {
    /** Bascule la réaction [emoji] de l'utilisateur courant : ajoute si absente, retire sinon. */
    fun toggleReaction(command: ToggleReactionCommand): ToggleReactionResult
}

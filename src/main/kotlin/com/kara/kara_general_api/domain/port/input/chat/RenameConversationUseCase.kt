package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.ConversationView
import com.kara.kara_general_api.domain.model.user.UserId

/**
 * Renomme une conversation. [title] vide ou absent restaure le titre déduit (salle et créneau pour une
 * conversation de réservation, noms des participants pour un groupe).
 */
data class RenameConversationCommand(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val title: String?,
)

sealed interface RenameConversationResult {
    data class Success(
        val conversation: ConversationView,
    ) : RenameConversationResult

    data object ConversationNotFound : RenameConversationResult

    data object NotParticipant : RenameConversationResult

    /**
     * Titre figé : une conversation à deux porte le nom de l'interlocuteur, et seul le client à
     * l'origine d'une réservation renomme la conversation qui lui est rattachée.
     */
    data object NotRenamable : RenameConversationResult

    data object TitleTooLong : RenameConversationResult
}

interface RenameConversationUseCase {
    fun renameConversation(command: RenameConversationCommand): RenameConversationResult
}

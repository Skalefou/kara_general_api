package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationView
import com.kara.kara_general_api.domain.model.user.UserId

/**
 * Supervision admin : liste toutes les conversations de la plateforme. [viewerId] (l'admin) sert au
 * calcul des vues (interlocuteur, non-lus) sans conférer de participation.
 */
interface ListAllConversationsUseCase {
    fun listAllConversations(viewerId: UserId): List<ConversationView>
}

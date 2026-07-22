package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationView
import com.kara.kara_general_api.domain.model.user.UserId

interface ListConversationsUseCase {
    fun listConversations(currentUserId: UserId): List<ConversationView>
}

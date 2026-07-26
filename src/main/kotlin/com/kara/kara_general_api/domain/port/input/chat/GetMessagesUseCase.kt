package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

data class GetMessagesQuery(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val limit: Int,
    val before: Instant?,
    /** Un administrateur peut lire n'importe quelle conversation sans en être participant. */
    val isAdmin: Boolean = false,
)

sealed interface GetMessagesResult {
    data class Success(
        val messages: List<MessageView>,
    ) : GetMessagesResult

    data object ConversationNotFound : GetMessagesResult

    data object NotParticipant : GetMessagesResult
}

interface GetMessagesUseCase {
    fun getMessages(query: GetMessagesQuery): GetMessagesResult
}

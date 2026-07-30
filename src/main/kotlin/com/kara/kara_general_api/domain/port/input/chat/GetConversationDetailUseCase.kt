package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole

/** Membre d'une conversation tel qu'affiché dans les paramètres du groupe. */
data class ConversationMemberView(
    val userId: UserId,
    val displayName: String,
    val photoKey: String?,
    val role: UserRole,
    val isAdmin: Boolean,
    val isMe: Boolean,
)

/**
 * Paramètres d'une conversation : titre, membres et droits du demandeur. Le client à l'origine d'une
 * réservation est administrateur de la conversation rattachée, sans avoir à être promu.
 */
data class ConversationDetailView(
    val id: ConversationId,
    val bookingId: BookingId?,
    val title: String,
    val isGroup: Boolean,
    val canRename: Boolean,
    val isAdmin: Boolean,
    val members: List<ConversationMemberView>,
)

data class GetConversationDetailQuery(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val isAdminRole: Boolean = false,
)

sealed interface GetConversationDetailResult {
    data class Success(
        val conversation: ConversationDetailView,
    ) : GetConversationDetailResult

    data object ConversationNotFound : GetConversationDetailResult

    data object NotParticipant : GetConversationDetailResult
}

interface GetConversationDetailUseCase {
    fun getConversationDetail(query: GetConversationDetailQuery): GetConversationDetailResult
}

package com.kara.kara_general_api.domain.port.input.chat

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.user.UserId

/** Promeut ou rétrograde un membre. Seul un administrateur du groupe en décide. */
data class SetConversationAdminCommand(
    val currentUserId: UserId,
    val conversationId: ConversationId,
    val memberId: UserId,
    val isAdmin: Boolean,
)

sealed interface SetConversationAdminResult {
    data class Success(
        val conversation: ConversationDetailView,
    ) : SetConversationAdminResult

    data object ConversationNotFound : SetConversationAdminResult

    data object NotParticipant : SetConversationAdminResult

    data object NotAdmin : SetConversationAdminResult

    data object MemberNotParticipant : SetConversationAdminResult

    /** Le client à l'origine de la réservation reste administrateur : sa promotion ne se retire pas. */
    data object CannotDemoteBookingOwner : SetConversationAdminResult
}

interface SetConversationAdminUseCase {
    fun setConversationAdmin(command: SetConversationAdminCommand): SetConversationAdminResult
}

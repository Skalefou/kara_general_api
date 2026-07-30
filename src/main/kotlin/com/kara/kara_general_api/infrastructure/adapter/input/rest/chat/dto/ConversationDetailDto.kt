package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

import com.kara.kara_general_api.domain.port.input.chat.ConversationDetailView

data class ConversationMemberDto(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: String,
    val isAdmin: Boolean,
    val isMe: Boolean,
)

data class ConversationDetailDto(
    val id: String,
    val bookingId: String?,
    val title: String,
    val isGroup: Boolean,
    val canRename: Boolean,
    val isAdmin: Boolean,
    val members: List<ConversationMemberDto>,
) {
    companion object {
        fun from(
            view: ConversationDetailView,
            photoUrl: (String) -> String,
        ): ConversationDetailDto =
            ConversationDetailDto(
                id = view.id.value.toString(),
                bookingId = view.bookingId?.value?.toString(),
                title = view.title,
                isGroup = view.isGroup,
                canRename = view.canRename,
                isAdmin = view.isAdmin,
                members =
                    view.members.map { member ->
                        ConversationMemberDto(
                            userId = member.userId.value.toString(),
                            displayName = member.displayName,
                            photoUrl = member.photoKey?.let(photoUrl),
                            role = member.role.name,
                            isAdmin = member.isAdmin,
                            isMe = member.isMe,
                        )
                    },
            )
    }
}

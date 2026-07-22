package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

import com.kara.kara_general_api.domain.model.chat.ConversationView
import java.time.format.DateTimeFormatter

data class ConversationDto(
    val id: String,
    val title: String,
    val counterpartName: String,
    val counterpartPhotoUrl: String?,
    val lastMessagePreview: String,
    val lastMessageAt: String,
    val isLastFromMe: Boolean,
    val unreadCount: Int,
) {
    companion object {
        fun from(view: ConversationView, photoUrl: (String) -> String): ConversationDto =
            ConversationDto(
                id = view.id.value.toString(),
                title = view.counterpartName,
                counterpartName = view.counterpartName,
                counterpartPhotoUrl = view.counterpartPhotoKey?.let(photoUrl),
                lastMessagePreview = view.lastMessagePreview,
                lastMessageAt = DateTimeFormatter.ISO_INSTANT.format(view.lastMessageAt),
                isLastFromMe = view.isLastFromMe,
                unreadCount = view.unreadCount,
            )
    }
}

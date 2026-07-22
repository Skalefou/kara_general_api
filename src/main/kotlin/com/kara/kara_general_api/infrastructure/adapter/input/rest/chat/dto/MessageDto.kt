package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

import com.kara.kara_general_api.domain.model.chat.MessageView
import java.time.format.DateTimeFormatter

data class ReplyToDto(
    val messageId: String,
    val senderName: String,
    val type: String,
    val preview: String,
)

data class ReactionDto(
    val emoji: String,
    val userId: String,
    val userName: String,
)

data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderPhotoUrl: String?,
    val type: String,
    val text: String?,
    val imageUrl: String?,
    val sentAt: String,
    val status: String,
    val isStaff: Boolean,
    val isPinned: Boolean,
    val isForwarded: Boolean,
    val replyTo: ReplyToDto?,
    val reactions: List<ReactionDto>,
) {
    companion object {
        fun from(view: MessageView, photoUrl: (String) -> String): MessageDto =
            MessageDto(
                id = view.message.id.value.toString(),
                conversationId = view.message.conversationId.value.toString(),
                senderId = view.message.senderId.value.toString(),
                senderName = view.senderName,
                senderPhotoUrl = view.senderPhotoKey?.let(photoUrl),
                type = view.message.type,
                text = view.message.text,
                imageUrl = null,
                sentAt = DateTimeFormatter.ISO_INSTANT.format(view.message.sentAt),
                status = view.status,
                isStaff = view.isStaff,
                isPinned = view.message.isPinned,
                isForwarded = view.message.isForwarded,
                replyTo =
                    view.replyTo?.let {
                        ReplyToDto(
                            messageId = it.messageId.value.toString(),
                            senderName = it.senderName,
                            type = it.type,
                            preview = it.preview,
                        )
                    },
                reactions =
                    view.reactions.map {
                        ReactionDto(
                            emoji = it.emoji,
                            userId = it.userId.value.toString(),
                            userName = it.userName,
                        )
                    },
            )
    }
}

package com.kara.kara_general_api.domain.model.chat

import com.kara.kara_general_api.domain.model.user.UserId

data class MessageReaction(
    val messageId: MessageId,
    val userId: UserId,
    val emoji: String,
)

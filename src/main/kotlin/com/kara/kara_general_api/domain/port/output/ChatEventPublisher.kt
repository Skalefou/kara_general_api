package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.chat.TypingEvent

/**
 * Port secondaire de diffusion temps réel. Implémenté par un adaptateur STOMP ; le domaine ignore
 * tout du broker de messages.
 */
interface ChatEventPublisher {
    fun publishMessage(
        conversationId: ConversationId,
        message: MessageView,
    )

    fun publishTyping(event: TypingEvent)
}

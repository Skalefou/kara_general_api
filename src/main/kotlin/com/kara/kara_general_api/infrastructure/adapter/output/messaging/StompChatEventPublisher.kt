package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.chat.MessageView
import com.kara.kara_general_api.domain.model.chat.TypingEvent
import com.kara.kara_general_api.domain.port.output.ChatEventPublisher
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.MessageDto
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.TypingDto
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.Duration

private val PHOTO_URL_TTL: Duration = Duration.ofMinutes(15)

/**
 * Adaptateur secondaire de diffusion STOMP. Convertit les vues du domaine en DTO (avec URL signées)
 * et les publie sur les topics « /topic/conversations/{id} » et « .../typing ».
 */
@Component
class StompChatEventPublisher(
    private val messagingTemplate: SimpMessagingTemplate,
    private val imageStorage: ImageStoragePort,
) : ChatEventPublisher {

    override fun publishMessage(conversationId: ConversationId, message: MessageView) {
        val dto = MessageDto.from(message) { key -> imageStorage.signedUrl(key, PHOTO_URL_TTL) }
        messagingTemplate.convertAndSend("/topic/conversations/${conversationId.value}", dto)
    }

    override fun publishTyping(event: TypingEvent) {
        val dto =
            TypingDto(
                conversationId = event.conversationId.value.toString(),
                userId = event.userId.value.toString(),
                userName = event.userName,
                isTyping = event.isTyping,
            )
        messagingTemplate.convertAndSend("/topic/conversations/${event.conversationId.value}/typing", dto)
    }
}

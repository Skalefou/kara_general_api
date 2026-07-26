package com.kara.kara_general_api.infrastructure.adapter.input.websocket

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.chat.NotifyTypingCommand
import com.kara.kara_general_api.domain.port.input.chat.NotifyTypingUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto.TypingRequest
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/**
 * Adaptateur primaire WebSocket. Reçoit les frames publiées sur « /app/conversations/{id}/typing »
 * et délègue au use case, qui rediffuse aux participants via le port de diffusion.
 */
@Controller
class ChatWebSocketController(
    private val notifyTypingUseCase: NotifyTypingUseCase,
) {
    @MessageMapping("/conversations/{id}/typing")
    fun typing(
        @DestinationVariable id: String,
        @Payload request: TypingRequest,
        principal: Principal,
    ) {
        notifyTypingUseCase.notifyTyping(
            NotifyTypingCommand(
                currentUserId = UserId(UUID.fromString(principal.name)),
                conversationId = ConversationId(UUID.fromString(id)),
                isTyping = request.isTyping,
            ),
        )
    }
}

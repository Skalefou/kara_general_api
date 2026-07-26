package com.kara.kara_general_api.infrastructure.config

import com.kara.kara_general_api.domain.model.chat.ConversationId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ChatRepository
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.UUID

private val CONVERSATION_TOPIC = Regex("^/topic/conversations/([^/]+)(?:/typing)?$")
private val SERVER_TOPIC = Regex("^/topic/servers/([^/]+)/(?:orders|emergency)$")

@Component
class StompSubscribeAuthorizationInterceptor(
    private val chatRepository: ChatRepository,
) : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        val accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                ?: return message
        if (StompCommand.SUBSCRIBE != accessor.command) return message

        val authentication =
            accessor.user as? Authentication ?: throw MessagingException("Unauthenticated subscription")
        val destination = accessor.destination ?: throw MessagingException("Missing subscription destination")
        if (!isAllowed(authentication, destination)) {
            throw MessagingException("Subscription denied")
        }
        return message
    }

    private fun isAllowed(
        authentication: Authentication,
        destination: String,
    ): Boolean {
        val currentUserId = parseUuid(authentication.name) ?: return false

        CONVERSATION_TOPIC.matchEntire(destination)?.let { match ->
            if (isAdmin(authentication)) return true
            val conversationId = parseUuid(match.groupValues[1]) ?: return false
            return chatRepository.isParticipant(ConversationId(conversationId), UserId(currentUserId))
        }

        SERVER_TOPIC.matchEntire(destination)?.let { match ->
            return parseUuid(match.groupValues[1]) == currentUserId
        }

        return false
    }

    private fun isAdmin(authentication: Authentication): Boolean = authentication.authorities.any { it.authority == "ROLE_ADMIN" }

    private fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()
}

package com.kara.kara_general_api.infrastructure.config

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

/**
 * Authentifie la frame STOMP CONNECT : lit l'en-tête natif « Authorization: Bearer <jwt> », le
 * valide avec le même parseur que le filtre HTTP, et pose le Principal (userId). La connexion est
 * refusée si le jeton est absent ou invalide.
 */
@Component
class StompAuthChannelInterceptor(
    private val tokenParser: JwtAccessTokenParser,
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                ?: return message
        if (StompCommand.CONNECT == accessor.command) {
            val header = accessor.getFirstNativeHeader("Authorization")
                ?: throw MessagingException("Missing Authorization header")
            if (!header.startsWith("Bearer ")) {
                throw MessagingException("Malformed Authorization header")
            }
            val user =
                tokenParser.parse(header.removePrefix("Bearer "))
                    ?: throw MessagingException("Invalid JWT")
            accessor.user =
                UsernamePasswordAuthenticationToken(
                    user.userId,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${user.role}")),
                )
        }
        return message
    }
}

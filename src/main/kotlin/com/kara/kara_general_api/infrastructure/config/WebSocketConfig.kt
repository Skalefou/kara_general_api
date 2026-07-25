package com.kara.kara_general_api.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configuration STOMP. Endpoint de handshake « /ws » (WebSocket brut). Broker simple en mémoire
 * sur « /topic », préfixe applicatif « /app ». L'authentification a lieu sur la frame CONNECT via
 * [StompAuthChannelInterceptor], l'autorisation des abonnements sur la frame SUBSCRIBE via
 * [StompSubscribeAuthorizationInterceptor].
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val stompAuthChannelInterceptor: StompAuthChannelInterceptor,
    private val stompSubscribeAuthorizationInterceptor: StompSubscribeAuthorizationInterceptor,
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthChannelInterceptor, stompSubscribeAuthorizationInterceptor)
    }
}

package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.kara.kara_general_api.domain.model.emergency.EmergencyAlert
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.EmergencyEventPublisher
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EmergencyDto
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * Diffuse les alertes d'urgence sur le topic « /topic/servers/{serverId}/emergency » : chaque serveur
 * s'abonne au sien pour recevoir ses alertes en temps réel.
 */
@Component
class StompEmergencyPublisher(
    private val messagingTemplate: SimpMessagingTemplate,
) : EmergencyEventPublisher {

    override fun publishEmergency(serverId: UserId, alert: EmergencyAlert) {
        val dto =
            EmergencyDto(
                bookingId = alert.bookingId.value.toString(),
                roomId = alert.roomId.value.toString(),
                roomName = alert.roomName,
                message = alert.message,
                triggeredAt = DateTimeFormatter.ISO_INSTANT.format(alert.triggeredAt),
            )
        messagingTemplate.convertAndSend("/topic/servers/${serverId.value}/emergency", dto)
    }
}

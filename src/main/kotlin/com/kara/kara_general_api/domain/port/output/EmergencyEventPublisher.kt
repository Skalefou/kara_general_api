package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.emergency.EmergencyAlert
import com.kara.kara_general_api.domain.model.user.UserId

/** Diffuse une alerte d'urgence à un serveur (temps réel). */
interface EmergencyEventPublisher {
    fun publishEmergency(serverId: UserId, alert: EmergencyAlert)
}

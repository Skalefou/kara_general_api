package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.order.OrderPlacedAlert
import com.kara.kara_general_api.domain.model.user.UserId

/** Diffuse à un serveur, en temps réel, l'alerte d'une nouvelle commande passée par un client. */
interface OrderPlacedEventPublisher {
    fun publishOrderPlaced(serverId: UserId, alert: OrderPlacedAlert)
}

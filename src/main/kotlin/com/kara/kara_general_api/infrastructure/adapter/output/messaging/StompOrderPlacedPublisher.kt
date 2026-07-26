package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.kara.kara_general_api.domain.model.order.OrderPlacedAlert
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.OrderPlacedEventPublisher
import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.OrderPlacedDto
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * Diffuse les alertes de nouvelle commande sur le topic « /topic/servers/{serverId}/orders » : chaque
 * serveur s'abonne au sien pour être averti (vibration + son) qu'un client vient de commander.
 */
@Component
class StompOrderPlacedPublisher(
    private val messagingTemplate: SimpMessagingTemplate,
) : OrderPlacedEventPublisher {
    override fun publishOrderPlaced(
        serverId: UserId,
        alert: OrderPlacedAlert,
    ) {
        val dto =
            OrderPlacedDto(
                orderId = alert.orderId.value.toString(),
                bookingId = alert.bookingId.value.toString(),
                roomId = alert.roomId.value.toString(),
                productName = alert.productName,
                quantity = alert.quantity,
                totalPrice = alert.totalPrice.toPlainString(),
                currency = alert.currency.name,
                placedAt = DateTimeFormatter.ISO_INSTANT.format(alert.placedAt),
            )
        messagingTemplate.convertAndSend("/topic/servers/${serverId.value}/orders", dto)
    }
}

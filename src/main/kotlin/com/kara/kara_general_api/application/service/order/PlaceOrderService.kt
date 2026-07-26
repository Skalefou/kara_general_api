package com.kara.kara_general_api.application.service.order

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.order.Order
import com.kara.kara_general_api.domain.model.order.OrderId
import com.kara.kara_general_api.domain.model.order.OrderPlacedAlert
import com.kara.kara_general_api.domain.model.order.OrderStatus
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderCommand
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderResult
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.OrderPlacedEventPublisher
import com.kara.kara_general_api.domain.port.output.OrderRepository
import com.kara.kara_general_api.domain.port.output.PaymentMethodPort
import com.kara.kara_general_api.domain.port.output.ProductRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Passe une commande de produit pendant une réservation active.
 *
 * Règles :
 * - Réservation « active » = statut CONFIRMED ET instant courant dans [startAt, endAt).
 * - La commande n'est autorisée qu'au propriétaire de la réservation.
 * - Le stock de la salle est décrémenté ; une quantité insuffisante refuse la commande.
 * - Un moyen de paiement enregistré est requis : sans lui, rien n'est persisté ni décrémenté et le client est
 *   invité à en mettre un en place. Le débit effectif (capture) est réalisé par la branche paiement, pas ici.
 */
@Service
class PlaceOrderService(
    private val bookingRepository: BookingRepository,
    private val productRepository: ProductRepository,
    private val roomStockRepository: RoomStockRepository,
    private val orderRepository: OrderRepository,
    private val paymentMethodPort: PaymentMethodPort,
    private val serverShiftRepository: ServerShiftRepository,
    private val orderPlacedEventPublisher: OrderPlacedEventPublisher,
) : PlaceOrderUseCase {

    @Transactional
    override fun placeOrder(command: PlaceOrderCommand): PlaceOrderResult {
        val booking =
            bookingRepository.findById(command.bookingId) ?: return PlaceOrderResult.BookingNotFound
        if (booking.userId != command.currentUserId) return PlaceOrderResult.NotOwner

        val now = Instant.now()
        val isActive =
            booking.status == BookingStatus.CONFIRMED &&
                !now.isBefore(booking.startAt) &&
                now.isBefore(booking.endAt)
        if (!isActive) return PlaceOrderResult.BookingNotActive

        val product =
            productRepository.findById(command.productId) ?: return PlaceOrderResult.ProductNotFound

        val available = roomStockRepository.findQuantity(booking.roomId, command.productId) ?: 0
        if (available < command.quantity) return PlaceOrderResult.InsufficientStock

        if (!paymentMethodPort.hasRegisteredPaymentMethod(command.currentUserId)) {
            return PlaceOrderResult.PaymentMethodRequired
        }

        if (!roomStockRepository.tryDecrement(booking.roomId, command.productId, command.quantity)) {
            return PlaceOrderResult.InsufficientStock
        }

        val unitPrice = product.price
        val order =
            Order(
                id = OrderId.generate(),
                bookingId = booking.id,
                userId = command.currentUserId,
                productId = command.productId,
                quantity = command.quantity,
                unitPrice = unitPrice,
                currency = product.currency,
                totalPrice = unitPrice.multiply(java.math.BigDecimal(command.quantity)),
                status = OrderStatus.PLACED,
                createdAt = now,
            )
        val saved = orderRepository.save(order)

        val alert =
            OrderPlacedAlert(
                orderId = saved.id,
                bookingId = booking.id,
                roomId = booking.roomId,
                productName = product.name,
                quantity = saved.quantity,
                totalPrice = saved.totalPrice,
                currency = saved.currency,
                placedAt = saved.createdAt,
            )
        serverShiftRepository
            .findServerIdsAssignedTo(booking.roomId, booking.startAt, booking.endAt)
            .forEach { serverId -> orderPlacedEventPublisher.publishOrderPlaced(serverId, alert) }

        return PlaceOrderResult.Success(saved)
    }
}

package com.kara.kara_general_api.infrastructure.adapter.input.rest.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderCommand
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderResult
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.OrderResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.PlaceOrderRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/orders")
class OrderController(
    private val placeOrderUseCase: PlaceOrderUseCase,
) : OrderApi {
    override fun placeOrder(
        bookingId: UUID,
        request: PlaceOrderRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            PlaceOrderCommand(
                bookingId = BookingId(bookingId),
                productId = ProductId(request.productId),
                quantity = request.quantity,
                currentUserId = UserId(UUID.fromString(authentication.name)),
            )
        return when (val result = placeOrderUseCase.placeOrder(command)) {
            is PlaceOrderResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(result.order))
            PlaceOrderResult.BookingNotFound -> bookingNotFound()
            PlaceOrderResult.NotOwner -> bookingNotOwner()
            PlaceOrderResult.BookingNotActive -> bookingNotActive()
            PlaceOrderResult.ProductNotFound -> productNotFound()
            PlaceOrderResult.InsufficientStock -> insufficientStock()
            PlaceOrderResult.PaymentMethodRequired -> paymentMethodRequired()
        }
    }

    private fun bookingNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucune réservation ne correspond à cet identifiant.",
                ).apply {
                    title = "Réservation introuvable"
                    setProperty("code", "BOOKING_NOT_FOUND")
                },
        )

    private fun productNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucun produit ne correspond à cet identifiant.",
                ).apply {
                    title = "Produit introuvable"
                    setProperty("code", "PRODUCT_NOT_FOUND")
                },
        )

    private fun bookingNotOwner(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "Cette réservation n'appartient pas à l'utilisateur courant.",
                ).apply {
                    title = "Accès refusé"
                    setProperty("code", "BOOKING_NOT_OWNER")
                },
        )

    private fun bookingNotActive(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "La réservation n'est pas active : une commande n'est possible que pendant le créneau confirmé.",
                ).apply {
                    title = "Réservation non active"
                    setProperty("code", "BOOKING_NOT_ACTIVE")
                },
        )

    private fun insufficientStock(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Le stock de la salle est insuffisant pour la quantité demandée.",
                ).apply {
                    title = "Stock insuffisant"
                    setProperty("code", "INSUFFICIENT_STOCK")
                },
        )

    private fun paymentMethodRequired(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Le paiement des commandes n'est pas encore disponible : Stripe reste à intégrer.",
                ).apply {
                    title = "Stripe à intégrer"
                    setProperty("code", "PAYMENT_METHOD_REQUIRED")
                },
        )
}

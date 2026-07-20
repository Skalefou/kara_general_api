package com.kara.kara_general_api.infrastructure.adapter.input.rest.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.payment.HandleStripeWebhookUseCase
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentResult
import com.kara.kara_general_api.domain.port.input.payment.InitiateBookingPaymentUseCase
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookCommand
import com.kara.kara_general_api.domain.port.input.payment.StripeWebhookResult
import com.kara.kara_general_api.infrastructure.adapter.input.rest.payment.dto.InitiateBookingPaymentResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PaymentController(
    private val initiateBookingPaymentUseCase: InitiateBookingPaymentUseCase,
    private val handleStripeWebhookUseCase: HandleStripeWebhookUseCase,
) : PaymentApi {

    override fun initiatePayment(id: UUID, authentication: Authentication): ResponseEntity<Any> {
        val command =
            InitiateBookingPaymentCommand(
                bookingId = BookingId(id),
                userId = UserId(UUID.fromString(authentication.name)),
            )
        return when (val result = initiateBookingPaymentUseCase.initiate(command)) {
            is InitiateBookingPaymentResult.Ready ->
                ResponseEntity.ok(InitiateBookingPaymentResponse.from(result))
            InitiateBookingPaymentResult.BookingNotFound -> bookingNotFound()
            InitiateBookingPaymentResult.NotOwner -> notOwner()
            InitiateBookingPaymentResult.AlreadyPaid -> alreadyPaid()
        }
    }

    override fun handleStripeWebhook(payload: String, signature: String?): ResponseEntity<Any> =
        when (handleStripeWebhookUseCase.handle(StripeWebhookCommand(payload = payload, signature = signature))) {
            StripeWebhookResult.Handled, StripeWebhookResult.Ignored -> ResponseEntity.ok().build()
            StripeWebhookResult.InvalidSignature ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "La signature du webhook Stripe est absente ou invalide.",
                    ).apply {
                        title = "Signature invalide"
                        setProperty("code", "STRIPE_INVALID_SIGNATURE")
                    },
                )
        }

    private fun bookingNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Aucune réservation ne correspond à cet identifiant.",
            ).apply {
                title = "Réservation introuvable"
                setProperty("code", "PAYMENT_BOOKING_NOT_FOUND")
            },
        )

    private fun notOwner(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Cette réservation n'appartient pas à votre compte.",
            ).apply {
                title = "Accès refusé"
                setProperty("code", "PAYMENT_NOT_OWNER")
            },
        )

    private fun alreadyPaid(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Cette réservation n'est plus en attente de paiement.",
            ).apply {
                title = "Paiement déjà effectué"
                setProperty("code", "PAYMENT_ALREADY_PAID")
            },
        )
}

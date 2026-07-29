package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.application.service.booking.ApplyBookingExtensionService
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Applique les effets d'un paiement « payer tout » réussi. Point d'entrée unique partagé par le webhook
 * Stripe ([StripeWebhookService]) et par la réconciliation appelée par le client
 * ([SyncBookingPaymentService]) : la confirmation ne dépend donc plus de la seule arrivée du webhook.
 *
 * Idempotence : elle porte sur l'**écriture du paiement** (on n'écrit pas deux fois PAID), jamais sur la
 * confirmation de la réservation. Un rejeu reconfirme donc toujours la réservation, ce qui rattrape un état
 * incohérent « paiement PAID mais réservation PENDING/CANCELLED ».
 *
 * Exception : un paiement REFUNDED correspond à une réservation annulée puis remboursée — un rejeu ne doit
 * jamais la ressusciter.
 *
 * Exécuté dans la transaction de l'appelant.
 */
@Service
class ConfirmPayAllPaymentService(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
    private val applyBookingExtensionService: ApplyBookingExtensionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Retourne true si les effets du paiement ont été appliqués, false si le paiement est remboursé. */
    fun confirm(payment: Payment): Boolean {
        if (payment.status == PaymentStatus.REFUNDED) {
            logger.info("Skipping pay-all confirmation: the payment has already been refunded")
            return false
        }

        if (payment.status != PaymentStatus.PAID) {
            paymentRepository.save(payment.markPaid())
        }

        val extensionId = payment.extensionId
        if (extensionId != null) {
            applyBookingExtensionService.apply(extensionId)
        } else {
            bookingRepository.updateStatus(payment.bookingId, BookingStatus.CONFIRMED)
        }
        return true
    }
}

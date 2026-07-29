package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.domain.port.input.payment.SyncBookingPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.SyncBookingPaymentResult
import com.kara.kara_general_api.domain.port.input.payment.SyncBookingPaymentUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Réconciliation d'un paiement « payer tout » demandée par le propriétaire de la réservation : on interroge
 * la passerelle pour connaître le statut **réel** du PaymentIntent et, s'il est réglé, on applique les mêmes
 * effets que le webhook via [ConfirmPayAllPaymentService]. Sinon rien n'est modifié.
 */
@Service
class SyncBookingPaymentService(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
    private val paymentGateway: PaymentGateway,
    private val confirmPayAllPaymentService: ConfirmPayAllPaymentService,
) : SyncBookingPaymentUseCase {
    @Transactional
    override fun sync(command: SyncBookingPaymentCommand): SyncBookingPaymentResult {
        val payment = paymentRepository.findById(command.paymentId) ?: return SyncBookingPaymentResult.NotFound
        if (payment.bookingId != command.bookingId) return SyncBookingPaymentResult.NotFound
        if (payment.userId != command.userId) return SyncBookingPaymentResult.NotOwner

        val snapshot = paymentGateway.retrievePaymentIntent(payment.stripePaymentIntentId)
        if (snapshot?.status == PaymentIntentStatus.SUCCEEDED) {
            confirmPayAllPaymentService.confirm(payment)
        }

        val booking = bookingRepository.findById(command.bookingId) ?: return SyncBookingPaymentResult.NotFound
        val currentPayment = paymentRepository.findById(command.paymentId) ?: payment
        return SyncBookingPaymentResult.Synced(
            bookingStatus = booking.status,
            paymentStatus = currentPayment.status,
        )
    }
}

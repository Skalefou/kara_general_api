package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.application.service.pool.PoolNotifier
import com.kara.kara_general_api.application.service.pool.PoolSettlementService
import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.PaymentStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingResult
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Annule une réservation appartenant au client, en libérant/remboursant les fonds selon l'état :
 *
 * - PENDING « payer tout » : aucune capture, rien à libérer → réservation CANCELLED.
 * - Cagnotte OUVERTE : lève toutes les autorisations Stripe (zéro prélèvement) → cagnotte + réservation
 *   CANCELLED. Réutilise la logique d'annulation de blocages de [PoolSettlementService].
 * - CONFIRMED (fonds capturés) : remboursement Stripe intégral — payer tout = le(s) Payment(s) PAID,
 *   cagnotte réglée = chaque part CAPTURED → Payment(s)/parts REFUNDED, réservation CANCELLED.
 *
 * Garde-fous : propriétaire uniquement, réservation non déjà annulée, début non encore passé.
 */
@Service
class CancelBookingService(
    private val bookingRepository: BookingRepository,
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentGateway: PaymentGateway,
    private val poolSettlementService: PoolSettlementService,
    private val poolNotifier: PoolNotifier,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val emailService: EmailService,
) : CancelBookingUseCase {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun cancel(command: CancelBookingCommand): CancelBookingResult {
        val booking = bookingRepository.findById(command.bookingId) ?: return CancelBookingResult.NotFound
        if (booking.userId != command.requesterId) return CancelBookingResult.NotOwner
        if (booking.status == BookingStatus.CANCELLED) return CancelBookingResult.AlreadyCancelled
        if (!booking.startAt.isAfter(Instant.now())) return CancelBookingResult.AlreadyStarted

        val pool = poolRepository.findByBookingId(booking.id)
        val refunded: Boolean =
            when {
                booking.status == BookingStatus.CONFIRMED -> {
                    if (pool != null) {
                        val shares = poolShareRepository.findByPoolId(pool.id)
                        poolSettlementService.refundCapturedShares(shares)
                        poolRepository.updateStatus(pool.id, PoolStatus.CANCELLED)
                        poolNotifier.notifyPoolCancelled(booking, shares)
                    } else {
                        refundPayAll(booking)
                        notifyOwner(booking, refunded = true)
                    }
                    true
                }

                pool != null && pool.isOpen() -> {
                    val shares = poolShareRepository.findByPoolId(pool.id)
                    poolSettlementService.cancelShareHolds(shares)
                    poolRepository.updateStatus(pool.id, PoolStatus.CANCELLED)
                    poolNotifier.notifyPoolCancelled(booking, shares)
                    false
                }

                else -> {
                    // PENDING « payer tout » (ou cagnotte non ouverte) : aucune capture, rien à libérer.
                    notifyOwner(booking, refunded = false)
                    false
                }
            }

        bookingRepository.updateStatus(booking.id, BookingStatus.CANCELLED)
        return CancelBookingResult.Cancelled(booking.cancel(), refunded)
    }

    private fun refundPayAll(booking: Booking) {
        paymentRepository.findByBookingId(booking.id)
            .filter { it.status == PaymentStatus.PAID }
            .forEach { payment ->
                runCatching { paymentGateway.refundPaymentIntent(payment.stripePaymentIntentId) }
                    .onFailure { logger.warn("Failed to refund a captured booking payment") }
                paymentRepository.save(payment.markRefunded())
            }
    }

    private fun notifyOwner(booking: Booking, refunded: Boolean) {
        val roomName = roomRepository.findById(booking.roomId)?.name ?: "votre réservation"
        val user = userRepository.findById(booking.userId) ?: return
        runCatching { emailService.sendBookingCancelled(user.email, roomName, booking.startAt, refunded) }
            .onFailure { logger.warn("Failed to send booking cancellation email") }
        user.fcmToken?.let { token ->
            runCatching {
                notificationService.sendPushNotification(
                    token = token,
                    title = "Réservation annulée",
                    body = if (refunded) "Votre réservation est annulée et remboursée." else "Votre réservation est annulée.",
                    data = mapOf("bookingId" to booking.id.value.toString(), "type" to "BOOKING_CANCELLED"),
                )
            }.onFailure { logger.warn("Failed to send booking cancellation push") }
        }
    }
}

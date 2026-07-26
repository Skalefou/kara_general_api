package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Centralise les notifications de cagnotte (email + push best-effort). Le push n'est envoyé que lorsqu'un
 * token d'appareil est connu (`User.fcmToken`) : la persistance des tokens n'étant pas encore branchée, le
 * push est donc silencieusement ignoré tant qu'aucun token n'est enregistré (cf. rapport). L'email, lui,
 * est toujours délivré. Les erreurs de notification ne remettent jamais en cause la transaction métier.
 */
@Component
class PoolNotifier(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val emailService: EmailService,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Cagnotte complète : email + push au créateur (propriétaire de la réservation). */
    fun notifyPoolConfirmed(booking: Booking) {
        val creator = userRepository.findById(booking.userId) ?: return
        val roomName = roomRepository.findById(booking.roomId)?.name ?: "votre réservation"
        runCatching { emailService.sendPoolConfirmation(creator.email, roomName, booking.startAt) }
            .onFailure { logger.warn("Failed to send pool confirmation email") }
        creator.fcmToken?.let { token ->
            runCatching {
                notificationService.sendPushNotification(
                    token = token,
                    title = "Cagnotte complète",
                    body = "Toutes les parts ont été payées : votre réservation est confirmée.",
                    data = mapOf("bookingId" to booking.id.value.toString(), "type" to "POOL_SETTLED"),
                )
            }.onFailure { logger.warn("Failed to send pool confirmation push") }
        }
    }

    /** Cagnotte expirée : email à chaque participant identifié + push best-effort aux payeurs connus. */
    fun notifyPoolCancelled(
        booking: Booking,
        shares: List<PoolShare>,
    ) {
        val roomName = roomRepository.findById(booking.roomId)?.name ?: "votre réservation"
        shares.forEach { share ->
            share.email?.let { email ->
                runCatching { emailService.sendPoolCancelled(email, share.participantName, roomName) }
                    .onFailure { logger.warn("Failed to send pool cancellation email") }
            }
            share.payerUserId?.let { payerId ->
                userRepository.findById(payerId)?.fcmToken?.let { token ->
                    runCatching {
                        notificationService.sendPushNotification(
                            token = token,
                            title = "Cagnotte annulée",
                            body = "Le délai est écoulé : la cagnotte est annulée, aucun montant n'a été prélevé.",
                            data = mapOf("bookingId" to booking.id.value.toString(), "type" to "POOL_EXPIRED"),
                        )
                    }.onFailure { logger.warn("Failed to send pool cancellation push") }
                }
            }
        }
    }
}

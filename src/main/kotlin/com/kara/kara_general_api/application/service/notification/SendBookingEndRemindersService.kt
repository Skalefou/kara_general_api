package com.kara.kara_general_api.application.service.notification

import com.kara.kara_general_api.domain.model.notification.BookingEndReminderKind
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderTarget
import com.kara.kara_general_api.domain.port.input.notification.SendBookingEndRemindersUseCase
import com.kara.kara_general_api.domain.port.output.BookingEndReminderRepository
import com.kara.kara_general_api.domain.port.output.NotificationService
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Envoie les rappels de fin de réservation (10 min puis 2 min avant la fin). Pour chaque type de rappel,
 * on récupère les réservations dont la fin tombe dans la fenêtre ]now, now + lead] et sans rappel déjà
 * envoyé, puis on notifie le client s'il possède un token FCM. Le rappel n'est marqué comme envoyé
 * qu'après un envoi réussi ; un client sans token n'est donc ni notifié ni marqué (nouvelle tentative
 * au prochain tick tant que la réservation reste dans la fenêtre).
 */
@Service
class SendBookingEndRemindersService(
    private val bookingEndReminderRepository: BookingEndReminderRepository,
    private val notificationService: NotificationService,
) : SendBookingEndRemindersUseCase {
    override fun sendDueReminders(now: Instant): Int {
        var sent = 0
        for (kind in BookingEndReminderKind.entries) {
            val to = now.plus(kind.lead)
            val targets = bookingEndReminderRepository.findConfirmedDue(kind, now, to)
            for (target in targets) {
                val token = target.fcmToken ?: continue
                notificationService.sendPushNotification(
                    token = token,
                    title = title(kind),
                    body = body(kind, target),
                    data =
                        mapOf(
                            "bookingId" to target.bookingId.value.toString(),
                            "kind" to kind.name,
                            "minutesBefore" to kind.minutesBefore.toString(),
                        ),
                )
                bookingEndReminderRepository.markSent(target.bookingId, kind)
                sent++
            }
        }
        return sent
    }

    private fun title(kind: BookingEndReminderKind): String =
        when (kind) {
            BookingEndReminderKind.TEN_MINUTES -> "Votre réservation se termine bientôt"
            BookingEndReminderKind.TWO_MINUTES -> "Fin imminente"
        }

    private fun body(
        kind: BookingEndReminderKind,
        target: BookingEndReminderTarget,
    ): String =
        when (kind) {
            BookingEndReminderKind.TEN_MINUTES ->
                "Il vous reste environ 10 minutes dans ${target.roomName}. Pensez à vous préparer à partir."

            BookingEndReminderKind.TWO_MINUTES ->
                "Il vous reste environ 2 minutes dans ${target.roomName}."
        }
}

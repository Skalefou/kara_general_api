package com.kara.kara_general_api.domain.model.notification

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

/**
 * Cible d'un rappel de fin de réservation : réservation CONFIRMED dont le créneau se termine bientôt,
 * enrichie du client destinataire (avec son [fcmToken], nullable tant qu'aucun appareil n'est enregistré)
 * et du nom de la salle [roomName] à afficher dans la notification.
 */
data class BookingEndReminderTarget(
    val bookingId: BookingId,
    val userId: UserId,
    val fcmToken: String?,
    val roomName: String,
    val endAt: Instant,
)

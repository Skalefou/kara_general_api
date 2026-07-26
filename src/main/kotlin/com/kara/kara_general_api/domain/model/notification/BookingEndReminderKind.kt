package com.kara.kara_general_api.domain.model.notification

import java.time.Duration

/**
 * Type de rappel de fin de réservation envoyé au client. Chaque type porte son délai d'anticipation
 * [lead] avant la fin du créneau (`endAt`) et le nombre de minutes correspondant [minutesBefore]
 * (repris dans le payload de la notification push).
 */
enum class BookingEndReminderKind(
    val lead: Duration,
    val minutesBefore: Int,
) {
    /** Rappel envoyé lorsqu'il reste environ 10 minutes avant la fin de la réservation. */
    TEN_MINUTES(Duration.ofMinutes(10), 10),

    /** Rappel envoyé lorsqu'il reste environ 2 minutes avant la fin de la réservation. */
    TWO_MINUTES(Duration.ofMinutes(2), 2),
}

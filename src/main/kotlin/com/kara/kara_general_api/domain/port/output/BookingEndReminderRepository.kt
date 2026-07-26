package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderKind
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderTarget
import java.time.Instant

/**
 * Port secondaire : sélection des réservations dont un rappel de fin est dû et suivi d'idempotence des
 * rappels déjà envoyés (table `booking_end_reminders`).
 */
interface BookingEndReminderRepository {
    /**
     * Retourne les réservations CONFIRMED dont la fin [BookingEndReminderTarget.endAt] tombe dans
     * l'intervalle ]from, to] et pour lesquelles aucun rappel de type [kind] n'a encore été envoyé.
     */
    fun findConfirmedDue(
        kind: BookingEndReminderKind,
        from: Instant,
        to: Instant,
    ): List<BookingEndReminderTarget>

    /** Marque le rappel [kind] comme envoyé pour la réservation [bookingId] (idempotent). */
    fun markSent(
        bookingId: BookingId,
        kind: BookingEndReminderKind,
    )

    fun deleteByBookingId(bookingId: BookingId)
}

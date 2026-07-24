package com.kara.kara_general_api.domain.port.input.notification

import java.time.Instant

/**
 * Envoie les rappels de fin de réservation (10 min et 2 min avant la fin) dus à l'instant [now].
 * Déclenché périodiquement par un planificateur.
 */
interface SendBookingEndRemindersUseCase {
    /** Retourne le nombre de notifications push envoyées. */
    fun sendDueReminders(now: Instant): Int
}

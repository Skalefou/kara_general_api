package com.kara.kara_general_api.domain.port.input.booking

import java.time.Instant

/**
 * Annule les réservations PENDING dont la fenêtre de paiement (15 min) est échue à [now], libérant
 * ainsi leur créneau. Déclenché périodiquement par un planificateur.
 */
interface CancelExpiredBookingsUseCase {
    /** Retourne le nombre de réservations annulées. */
    fun cancelExpired(now: Instant): Int
}

package com.kara.kara_general_api.domain.port.input.pool

import java.time.Instant

/**
 * Balaie les cagnottes OPEN dont le délai est échu : annule toutes les autorisations Stripe (zéro
 * prélèvement), passe la cagnotte EXPIRED et la réservation CANCELLED, puis notifie les participants.
 * Retourne le nombre de cagnottes expirées.
 */
interface CancelExpiredPoolsUseCase {
    fun cancelExpired(now: Instant): Int
}

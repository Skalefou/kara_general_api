package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

interface PoolRepository {
    /** Persiste (upsert) la cagnotte. */
    fun save(pool: Pool): Pool

    fun findById(id: PoolId): Pool?

    fun findByBookingId(bookingId: BookingId): Pool?

    /** Retrouve la cagnotte par son token de lien global (recap public « rejoindre la cagnotte »). */
    fun findByGlobalLinkToken(token: String): Pool?

    fun updateStatus(id: PoolId, status: PoolStatus)

    fun updateGlobalLinkToken(id: PoolId, token: String)

    /** Cagnottes OPEN dont le délai est échu ([deadline] <= [now]) — balayage d'expiration. */
    fun findExpiredOpen(now: Instant): List<Pool>

    /**
     * Cagnottes auxquelles l'utilisateur participe : celles qu'il a créées (propriétaire de la réservation)
     * OU dont il détient une part (payeur). Triées par échéance puis création décroissantes.
     */
    fun findByUserInvolvement(userId: UserId): List<Pool>
}

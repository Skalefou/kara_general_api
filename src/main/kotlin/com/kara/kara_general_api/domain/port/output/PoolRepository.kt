package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
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

    /**
     * Verrou pessimiste (`SELECT ... FOR UPDATE`) sur la ligne de la cagnotte. Sérialise **tout** le
     * règlement d'une même cagnotte : le webhook Stripe et la réconciliation demandée par le front prennent
     * ce verrou avant de décider d'une transition, si bien qu'ils ne peuvent ni autoriser deux fois la même
     * part, ni capturer deux fois, ni conclure tous les deux à la complétude. À appeler **dans une
     * transaction**, et toujours **avant** de relire les parts. Retourne null si la cagnotte n'existe pas.
     */
    fun findByIdForUpdate(id: PoolId): Pool?

    fun findByBookingId(bookingId: BookingId): Pool?

    fun findByExtensionId(extensionId: BookingExtensionId): Pool?

    /**
     * Cagnottes de réservation (hors cagnottes d'extension) rattachées à [bookingIds], en **une seule**
     * requête. Retourne une liste vide sans interroger la base si [bookingIds] est vide.
     */
    fun findByBookingIds(bookingIds: List<BookingId>): List<Pool>

    /** Retrouve la cagnotte par son token de lien global (recap public « rejoindre la cagnotte »). */
    fun findByGlobalLinkToken(token: String): Pool?

    fun updateStatus(
        id: PoolId,
        status: PoolStatus,
    )

    fun updateGlobalLinkToken(
        id: PoolId,
        token: String,
    )

    /** Cagnottes OPEN dont le délai est échu ([deadline] <= [now]) — balayage d'expiration. */
    fun findExpiredOpen(now: Instant): List<Pool>

    /**
     * Cagnottes auxquelles l'utilisateur participe : celles qu'il a créées (propriétaire de la réservation)
     * OU dont il détient une part (payeur). Triées par échéance puis création décroissantes.
     */
    fun findByUserInvolvement(userId: UserId): List<Pool>
}

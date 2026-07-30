package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId

interface PoolShareRepository {
    /** Persiste (upsert) une part. */
    fun save(share: PoolShare): PoolShare

    /** Persiste (upsert) un lot de parts (création de la cagnotte). */
    fun saveAll(shares: List<PoolShare>): List<PoolShare>

    fun findById(id: PoolShareId): PoolShare?

    fun findByPoolId(poolId: PoolId): List<PoolShare>

    /**
     * Parts de plusieurs cagnottes en **une seule** requête (évite le N+1 sur une liste de cagnottes).
     * Retourne une liste vide sans interroger la base si [poolIds] est vide.
     */
    fun findByPoolIds(poolIds: List<PoolId>): List<PoolShare>

    /**
     * Verrou pessimiste (`SELECT ... FOR UPDATE`) sur la part reliquat du créateur d'une cagnotte. Sérialise
     * les auto-inscriptions concurrentes : tant que la transaction détentrice n'a pas commité, toute autre
     * lecture verrouillée du même reliquat attend, garantissant l'invariant somme(parts) == cible. À appeler
     * **dans une transaction**. Retourne null si la cagnotte n'a pas de part créateur.
     */
    fun findCreatorShareForUpdate(poolId: PoolId): PoolShare?

    /** Retrouve la part par son token de lien unique (recap public « payer ma part »). */
    fun findByUniqueLinkToken(token: String): PoolShare?

    /**
     * Part d'une cagnotte dont [payerUserId] est le payeur, s'il en détient une. Sert au récapitulatif via le
     * lien global lorsqu'un appelant authentifié doit retrouver **sa** part pour reprendre un paiement
     * interrompu. Le filtrage sur le payeur est fait en SQL : la part d'un tiers n'est jamais retournée.
     *
     * Rien ne garantit l'unicité en base (un même utilisateur peut légitimement régler deux parts d'une même
     * cagnotte, par exemple sa part et le reliquat du créateur) : la plus ancienne est retournée.
     */
    fun findByPoolIdAndPayerUserId(
        poolId: PoolId,
        payerUserId: UserId,
    ): PoolShare?

    /**
     * Vrai si [payerUserId] détient au moins une part d'une cagnotte rattachée à la réservation [bookingId].
     * C'est le test d'« implication » d'un participant dans une réservation dont il n'est pas l'organisateur :
     * il ouvre la lecture (liste et détail de la réservation), jamais l'écriture.
     */
    fun existsForBookingAndPayer(
        bookingId: BookingId,
        payerUserId: UserId,
    ): Boolean

    /** Retrouve la part par l'identifiant de son PaymentIntent Stripe (clé utilisée par le webhook). */
    fun findByStripePaymentIntentId(stripePaymentIntentId: String): PoolShare?
}

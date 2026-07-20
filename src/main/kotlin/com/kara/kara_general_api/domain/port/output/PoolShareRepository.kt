package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId

interface PoolShareRepository {
    /** Persiste (upsert) une part. */
    fun save(share: PoolShare): PoolShare

    /** Persiste (upsert) un lot de parts (création de la cagnotte). */
    fun saveAll(shares: List<PoolShare>): List<PoolShare>

    fun findById(id: PoolShareId): PoolShare?

    fun findByPoolId(poolId: PoolId): List<PoolShare>

    /** Retrouve la part par son token de lien unique (recap public « payer ma part »). */
    fun findByUniqueLinkToken(token: String): PoolShare?

    /** Retrouve la part par l'identifiant de son PaymentIntent Stripe (clé utilisée par le webhook). */
    fun findByStripePaymentIntentId(stripePaymentIntentId: String): PoolShare?
}

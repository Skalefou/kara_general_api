package com.kara.kara_general_api.domain.model.payment

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import java.math.BigDecimal

/**
 * Part d'un participant à une cagnotte. Son montant est réglé par une autorisation Stripe à capture
 * manuelle ([stripePaymentIntentId]). Un lien unique ([uniqueLinkToken]) est généré lorsqu'un email est
 * connu, pour inviter nommément le participant. [isCreatorShare] identifie le reliquat du créateur.
 */
data class PoolShare(
    val id: PoolShareId,
    val poolId: PoolId,
    val participantName: String,
    val email: Email?,
    val amount: BigDecimal,
    val status: PoolShareStatus,
    val stripePaymentIntentId: String?,
    val uniqueLinkToken: String?,
    val payerUserId: UserId?,
    val isCreatorShare: Boolean,
) {
    fun isSettleable(): Boolean = status == PoolShareStatus.AUTHORIZED || status == PoolShareStatus.CAPTURED

    /** Associe l'autorisation Stripe (PaymentIntent à capture manuelle) et son payeur. La part reste PENDING
     *  jusqu'à confirmation du blocage des fonds par le webhook `amount_capturable_updated`. */
    fun withAuthorizationIntent(
        intentId: String,
        payerUserId: UserId,
    ): PoolShare = copy(stripePaymentIntentId = intentId, payerUserId = payerUserId)

    /**
     * Détache l'autorisation Stripe de la part, sans toucher à son payeur. À utiliser lorsque le montant dû
     * par la part change : une autorisation à capture manuelle est figée sur le montant demandé à sa création,
     * elle ne couvre donc plus le nouveau dû et ne doit surtout pas être capturée. La part redevient une part
     * PENDING sans autorisation, que le payeur règle à nouveau au bon montant.
     */
    fun withoutAuthorizationIntent(): PoolShare = copy(stripePaymentIntentId = null)

    fun markAuthorized(): PoolShare = copy(status = PoolShareStatus.AUTHORIZED)

    fun markCaptured(): PoolShare = copy(status = PoolShareStatus.CAPTURED)

    fun markCancelled(): PoolShare = copy(status = PoolShareStatus.CANCELLED)

    fun markRefunded(): PoolShare = copy(status = PoolShareStatus.REFUNDED)

    fun updateAmount(newAmount: BigDecimal): PoolShare {
        require(newAmount > BigDecimal.ZERO) { "Le montant d'une part doit être strictement positif" }
        return copy(amount = newAmount)
    }

    companion object {
        /** Crée une part PENDING, sans autorisation Stripe ni payeur (créés lors du paiement de la part). */
        fun create(
            poolId: PoolId,
            participantName: String,
            email: Email?,
            amount: BigDecimal,
            uniqueLinkToken: String?,
            isCreatorShare: Boolean,
        ): PoolShare {
            require(amount > BigDecimal.ZERO) { "Le montant d'une part doit être strictement positif" }
            require(participantName.isNotBlank()) { "Le nom du participant est obligatoire" }
            return PoolShare(
                id = PoolShareId.generate(),
                poolId = poolId,
                participantName = participantName,
                email = email,
                amount = amount,
                status = PoolShareStatus.PENDING,
                stripePaymentIntentId = null,
                uniqueLinkToken = uniqueLinkToken,
                payerUserId = null,
                isCreatorShare = isCreatorShare,
            )
        }
    }
}

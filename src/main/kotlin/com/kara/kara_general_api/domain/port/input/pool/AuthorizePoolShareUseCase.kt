package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId
import java.util.UUID

data class AuthorizePoolShareCommand(
    val poolId: PoolId,
    val shareId: PoolShareId,
    val payerId: UserId,
)

sealed interface AuthorizePoolShareResult {
    /** Secrets destinés au PaymentSheet Stripe (autorisation à capture manuelle). */
    data class Ready(
        val clientSecret: String,
        val ephemeralKeySecret: String,
        val customerId: String,
        val publishableKey: String,
        val shareId: UUID,
    ) : AuthorizePoolShareResult

    data object PoolNotFound : AuthorizePoolShareResult

    data object ShareNotFound : AuthorizePoolShareResult

    /** La cagnotte n'est plus ouverte (déjà réglée, annulée ou expirée). */
    data object PoolClosed : AuthorizePoolShareResult

    /** Le délai de la cagnotte est écoulé. */
    data object PoolExpired : AuthorizePoolShareResult

    /** La part n'est plus à payer (déjà autorisée, capturée ou annulée). */
    data object ShareAlreadyProcessed : AuthorizePoolShareResult

    data object PayerNotFound : AuthorizePoolShareResult
}

interface AuthorizePoolShareUseCase {
    fun authorize(command: AuthorizePoolShareCommand): AuthorizePoolShareResult
}

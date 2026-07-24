package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareUseCase
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Autorise (bloque) le montant d'une part via un PaymentIntent Stripe en **capture manuelle** : aucun
 * prélèvement n'a lieu ici. Les secrets retournés alimentent le PaymentSheet côté front. Le passage de la
 * part à AUTHORIZED (fonds bloqués), la vérification de complétude et la capture globale sont pilotés par
 * le webhook Stripe (`amount_capturable_updated`).
 */
@Service
class AuthorizePoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
) : AuthorizePoolShareUseCase {

    @Transactional
    override fun authorize(command: AuthorizePoolShareCommand): AuthorizePoolShareResult {
        val pool = poolRepository.findById(command.poolId) ?: return AuthorizePoolShareResult.PoolNotFound
        if (!pool.isOpen()) return AuthorizePoolShareResult.PoolClosed
        if (pool.isExpired(Instant.now())) return AuthorizePoolShareResult.PoolExpired

        val share = poolShareRepository.findById(command.shareId) ?: return AuthorizePoolShareResult.ShareNotFound
        if (share.poolId != pool.id) return AuthorizePoolShareResult.ShareNotFound
        if (share.status != PoolShareStatus.PENDING) return AuthorizePoolShareResult.ShareAlreadyProcessed

        val payer = userRepository.findById(command.payerId) ?: return AuthorizePoolShareResult.PayerNotFound

        val customerId = paymentGateway.ensureCustomer(payer)
        if (payer.stripeCustomerId != customerId) {
            userRepository.updateStripeCustomerId(payer.id, customerId)
        }
        val ephemeralKeySecret = paymentGateway.createEphemeralKey(customerId)
        val intent = paymentGateway.createManualCapturePaymentIntent(share.amount, pool.currency, customerId)

        poolShareRepository.save(share.withAuthorizationIntent(intent.paymentIntentId, payer.id))

        return AuthorizePoolShareResult.Ready(
            clientSecret = intent.clientSecret,
            ephemeralKeySecret = ephemeralKeySecret,
            customerId = customerId,
            publishableKey = paymentGateway.publishableKey(),
            shareId = share.id.value,
        )
    }
}

package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareUseCase
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Auto-inscription d'un utilisateur authentifié à une cagnotte via le lien global, en un seul appel
 * atomique : découpe (carve) du reliquat du créateur pour financer la nouvelle part, puis autorisation
 * Stripe (capture manuelle) — combinant la logique de [AddPoolShareService] (sans la garde « créateur
 * uniquement ») et de [AuthorizePoolShareService].
 *
 * Concurrence : le reliquat du créateur est **verrouillé** (`FOR UPDATE`) avant lecture de son montant, si
 * bien que deux auto-inscriptions simultanées se sérialisent et ne peuvent pas sur-découper le reliquat.
 */
@Service
class SelfJoinPoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
) : SelfJoinPoolShareUseCase {

    @Transactional
    override fun selfJoin(command: SelfJoinPoolShareCommand): SelfJoinPoolShareResult {
        val pool = poolRepository.findByGlobalLinkToken(command.globalToken)
            ?: return SelfJoinPoolShareResult.PoolNotFound
        if (!pool.isOpen()) return SelfJoinPoolShareResult.PoolClosed
        if (pool.isExpired(Instant.now())) return SelfJoinPoolShareResult.PoolExpired

        val user = userRepository.findById(command.callerId) ?: return SelfJoinPoolShareResult.PayerNotFound

        // Règle « une part par personne » : l'utilisateur ne doit pas déjà détenir une part de cette cagnotte.
        if (poolShareRepository.findByPoolId(pool.id).any { it.payerUserId == command.callerId }) {
            return SelfJoinPoolShareResult.AlreadyJoined
        }

        // Verrou pessimiste AVANT lecture du montant : sérialise les auto-inscriptions concurrentes.
        val creatorShare = poolShareRepository.findCreatorShareForUpdate(pool.id)
            ?: return SelfJoinPoolShareResult.NoCreatorRemainder
        if (creatorShare.status != PoolShareStatus.PENDING) return SelfJoinPoolShareResult.RemainderLocked
        if (command.amount <= BigDecimal.ZERO) return SelfJoinPoolShareResult.InvalidAmount
        val remaining = creatorShare.amount - command.amount
        if (remaining <= BigDecimal.ZERO) return SelfJoinPoolShareResult.InsufficientRemainder

        // Carve : le reliquat du créateur finance la nouvelle part (invariant somme == cible préservé).
        poolShareRepository.save(creatorShare.updateAmount(remaining))
        val selfShare =
            PoolShare.create(
                poolId = pool.id,
                participantName = "${user.firstName} ${user.lastName}",
                email = user.email,
                amount = command.amount,
                uniqueLinkToken = null,
                isCreatorShare = false,
            )

        // Autorisation Stripe (capture manuelle) : aucun prélèvement ici ; la part reste PENDING jusqu'au
        // webhook `amount_capturable_updated`.
        val customerId = paymentGateway.ensureCustomer(user)
        if (user.stripeCustomerId != customerId) {
            userRepository.updateStripeCustomerId(user.id, customerId)
        }
        val ephemeralKeySecret = paymentGateway.createEphemeralKey(customerId)
        val intent = paymentGateway.createManualCapturePaymentIntent(command.amount, pool.currency, customerId)
        poolShareRepository.save(selfShare.withAuthorizationIntent(intent.paymentIntentId, user.id))

        return SelfJoinPoolShareResult.Ready(
            clientSecret = intent.clientSecret,
            ephemeralKeySecret = ephemeralKeySecret,
            customerId = customerId,
            publishableKey = paymentGateway.publishableKey(),
            shareId = selfShare.id.value,
        )
    }
}

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
 * Concurrence : la cagnotte puis le reliquat du créateur sont **verrouillés** (`FOR UPDATE`) avant lecture du
 * montant du reliquat, si bien que deux auto-inscriptions simultanées se sérialisent et ne peuvent pas
 * sur-découper le reliquat. Le verrou de cagnotte est le même que celui du règlement
 * ([PoolSettlementService]) et de l'autorisation d'une part ([AuthorizePoolShareService]) : découpe et
 * règlement ne peuvent donc pas s'entrelacer et s'écraser mutuellement (l'upsert d'une part réécrit toute la
 * ligne, montant compris). Il est pris en premier partout, ce qui exclut tout interblocage.
 *
 * `rollbackFor = Exception::class` est **impératif** : `com.stripe.exception.StripeException` étend
 * `java.lang.Exception` (exception **vérifiée**), or Spring ne déclenche par défaut le rollback que sur
 * `RuntimeException`/`Error`. Sans cette mention, un échec de la passerelle (`ensureCustomer`,
 * `createEphemeralKey`, `createManualCapturePaymentIntent`) **commiterait** la découpe du reliquat du créateur
 * sans créer la part financée : l'invariant somme(parts) == targetAmount serait cassé définitivement et la
 * cagnotte ne pourrait plus jamais être complétée ([PoolSettlementService] ne la verrait jamais complète).
 */
@Service
class SelfJoinPoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
    private val poolShareHoldReleaser: PoolShareHoldReleaser,
) : SelfJoinPoolShareUseCase {
    @Transactional(rollbackFor = [Exception::class])
    override fun selfJoin(command: SelfJoinPoolShareCommand): SelfJoinPoolShareResult {
        val located =
            poolRepository.findByGlobalLinkToken(command.globalToken)
                ?: return SelfJoinPoolShareResult.PoolNotFound
        // Relecture verrouillée : l'état décidé ci-dessous doit être celui commité, pas un instantané périmé.
        val pool = poolRepository.findByIdForUpdate(located.id) ?: return SelfJoinPoolShareResult.PoolNotFound
        if (!pool.isOpen()) return SelfJoinPoolShareResult.PoolClosed
        if (pool.isExpired(Instant.now())) return SelfJoinPoolShareResult.PoolExpired

        val user = userRepository.findById(command.callerId) ?: return SelfJoinPoolShareResult.PayerNotFound

        // Règle « une part par personne » : l'utilisateur ne doit pas déjà détenir une part de cette cagnotte.
        if (poolShareRepository.findByPoolId(pool.id).any { it.payerUserId == command.callerId }) {
            return SelfJoinPoolShareResult.AlreadyJoined
        }

        // Verrou pessimiste AVANT lecture du montant : sérialise les auto-inscriptions concurrentes.
        val creatorShare =
            poolShareRepository.findCreatorShareForUpdate(pool.id)
                ?: return SelfJoinPoolShareResult.NoCreatorRemainder
        if (creatorShare.status != PoolShareStatus.PENDING) return SelfJoinPoolShareResult.RemainderLocked
        if (command.amount <= BigDecimal.ZERO) return SelfJoinPoolShareResult.InvalidAmount
        val remaining = creatorShare.amount - command.amount
        if (remaining <= BigDecimal.ZERO) return SelfJoinPoolShareResult.InsufficientRemainder

        // Carve : le reliquat du créateur finance la nouvelle part (invariant somme == cible préservé). Son
        // montant change, donc l'autorisation Stripe qu'il porte éventuellement (le créateur a ouvert le
        // PaymentSheet de son reliquat sans que l'autorisation soit encore enregistrée AUTHORIZED) ne couvre
        // plus le dû : elle est libérée et détachée, jamais capturée.
        poolShareRepository.save(poolShareHoldReleaser.release(creatorShare).updateAmount(remaining))
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

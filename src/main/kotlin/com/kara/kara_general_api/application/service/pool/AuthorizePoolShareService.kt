package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareUseCase
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentSnapshot
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Autorise (bloque) le montant d'une part via un PaymentIntent Stripe en **capture manuelle** : aucun
 * prélèvement n'a lieu ici. Les secrets retournés alimentent le PaymentSheet côté front. Le passage de la
 * part à AUTHORIZED (fonds bloqués), la vérification de complétude et la capture globale sont pilotés par
 * le webhook Stripe (`amount_capturable_updated`).
 *
 * Aucune restriction de propriété : tout utilisateur authentifié peut régler toute part PENDING d'une cagnotte
 * ouverte — un participant invité par lien n'est pas propriétaire de la cagnotte, et le créateur règle sa part
 * de reliquat (`isCreatorShare`) par ce même chemin, sans traitement particulier.
 *
 * **Reprise d'un paiement interrompu** : la part peut déjà porter une autorisation quand le PaymentSheet a
 * échoué ou a été abandonné côté front. L'ignorer serait doublement dangereux, car l'upsert d'une part réécrit
 * la colonne `stripe_payment_intent_id` : l'ancien intent deviendrait orphelin (introuvable par
 * [PoolShareRepository.findByStripePaymentIntentId], donc le webhook `amount_capturable_updated` le concernant
 * serait un no-op silencieux) et la carte du participant pourrait porter **deux** blocages. Trois cas :
 *
 * - fonds déjà bloqués (`requires_capture`) ou déjà prélevés (`succeeded`) → [AuthorizePoolShareResult.ShareAlreadyProcessed] :
 *   il n'y a rien à repayer, le front réconcilie l'état via `POST /pools/{poolId}/shares/{shareId}/sync` ;
 * - intent encore payable ([PaymentIntentStatus.isReusableForPayment]) → **le même** client secret est re-servi,
 *   sans aucune écriture en base (même logique que
 *   [com.kara.kara_general_api.application.service.payment.InitiateBookingPaymentService] pour « payer tout ») ;
 * - tout autre cas (`requires_action`, `canceled`, ou passerelle injoignable — la relecture retourne `null` sans
 *   lever) → l'ancien blocage est libéré et détaché par [PoolShareHoldReleaser] **avant** la création du neuf.
 *
 * Concurrence : la cagnotte est **verrouillée** ([PoolRepository.findByIdForUpdate]) avant lecture de la part,
 * comme dans [PoolSettlementService] et dans les services qui découpent le reliquat. Sans ce verrou, une
 * découpe concurrente serait écrasée par la réécriture complète de la ligne de part (l'upsert réécrit aussi le
 * montant), cassant l'invariant somme(parts) == cible.
 *
 * `rollbackFor = Exception::class` est **impératif** : `com.stripe.exception.StripeException` étend
 * `java.lang.Exception` (exception **vérifiée**), or Spring ne déclenche par défaut le rollback que sur
 * `RuntimeException`/`Error`. Ce service écrit en base (identifiant client Stripe, détachement de l'ancien
 * blocage) puis rappelle la passerelle : sans cette mention, un échec Stripe commiterait des écritures
 * partielles.
 */
@Service
class AuthorizePoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
    private val poolShareHoldReleaser: PoolShareHoldReleaser,
) : AuthorizePoolShareUseCase {
    @Transactional(rollbackFor = [Exception::class])
    override fun authorize(command: AuthorizePoolShareCommand): AuthorizePoolShareResult {
        // Verrou pessimiste AVANT lecture de la part : sérialise avec le règlement et avec toute découpe.
        val pool = poolRepository.findByIdForUpdate(command.poolId) ?: return AuthorizePoolShareResult.PoolNotFound
        if (!pool.isOpen()) return AuthorizePoolShareResult.PoolClosed
        if (pool.isExpired(Instant.now())) return AuthorizePoolShareResult.PoolExpired

        val share = poolShareRepository.findById(command.shareId) ?: return AuthorizePoolShareResult.ShareNotFound
        if (share.poolId != pool.id) return AuthorizePoolShareResult.ShareNotFound
        if (share.status != PoolShareStatus.PENDING) return AuthorizePoolShareResult.ShareAlreadyProcessed

        // Statut réel, chez la passerelle, de l'autorisation que la part porte éventuellement déjà.
        val existing = share.stripePaymentIntentId?.let { paymentGateway.retrievePaymentIntent(it) }
        if (existing != null && existing.holdsFunds()) return AuthorizePoolShareResult.ShareAlreadyProcessed

        val payer = userRepository.findById(command.payerId) ?: return AuthorizePoolShareResult.PayerNotFound

        val customerId = paymentGateway.ensureCustomer(payer)
        if (payer.stripeCustomerId != customerId) {
            userRepository.updateStripeCustomerId(payer.id, customerId)
        }
        val ephemeralKeySecret = paymentGateway.createEphemeralKey(customerId)

        // Intent encore payable : on re-sert son client secret tel quel. Aucune écriture, donc aucun risque
        // d'orpheliner l'intent en cours ni de créer un second blocage sur la même carte.
        existing?.reusableClientSecret()?.let { clientSecret ->
            return ready(clientSecret, ephemeralKeySecret, customerId, share.id.value)
        }

        // Intent inexploitable (requires_action, canceled) ou passerelle injoignable : l'ancien blocage est
        // libéré et détaché AVANT la création du neuf. Sans effet sur une part sans autorisation.
        val releasedShare = poolShareHoldReleaser.release(share)
        val intent = paymentGateway.createManualCapturePaymentIntent(releasedShare.amount, pool.currency, customerId)

        poolShareRepository.save(releasedShare.withAuthorizationIntent(intent.paymentIntentId, payer.id))

        return ready(intent.clientSecret, ephemeralKeySecret, customerId, share.id.value)
    }

    /** Fonds déjà bloqués ou déjà prélevés : la part n'est plus à payer, seule la réconciliation reste à faire. */
    private fun PaymentIntentSnapshot.holdsFunds(): Boolean =
        status == PaymentIntentStatus.REQUIRES_CAPTURE || status == PaymentIntentStatus.SUCCEEDED

    /** Client secret à re-servir tel quel, ou null si l'intent n'est plus payable en l'état. */
    private fun PaymentIntentSnapshot.reusableClientSecret(): String? = clientSecret?.takeIf { status.isReusableForPayment() }

    private fun ready(
        clientSecret: String,
        ephemeralKeySecret: String,
        customerId: String,
        shareId: UUID,
    ): AuthorizePoolShareResult.Ready =
        AuthorizePoolShareResult.Ready(
            clientSecret = clientSecret,
            ephemeralKeySecret = ephemeralKeySecret,
            customerId = customerId,
            publishableKey = paymentGateway.publishableKey(),
            shareId = shareId,
        )
}

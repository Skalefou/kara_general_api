package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.util.UUID

/**
 * Un utilisateur authentifié rejoint lui-même une cagnotte via le lien global : il crée sa propre part
 * (montant prélevé sur le reliquat du créateur, plafonné par ce reliquat) et obtient les secrets Stripe du
 * PaymentSheet — le tout dans un seul appel atomique. Le nom et l'email de la part sont dérivés du compte.
 */
data class SelfJoinPoolShareCommand(
    val globalToken: String,
    val callerId: UserId,
    val amount: BigDecimal,
)

sealed interface SelfJoinPoolShareResult {
    /** Secrets destinés au PaymentSheet Stripe (autorisation à capture manuelle sur la part créée). */
    data class Ready(
        val clientSecret: String,
        val ephemeralKeySecret: String,
        val customerId: String,
        val publishableKey: String,
        val shareId: UUID,
    ) : SelfJoinPoolShareResult

    data object PoolNotFound : SelfJoinPoolShareResult

    /** La cagnotte n'est plus ouverte (déjà réglée, annulée ou expirée). */
    data object PoolClosed : SelfJoinPoolShareResult

    /** Le délai de la cagnotte est écoulé. */
    data object PoolExpired : SelfJoinPoolShareResult

    data object PayerNotFound : SelfJoinPoolShareResult

    /** L'utilisateur détient déjà une part dans cette cagnotte (règle : une seule part par personne). */
    data object AlreadyJoined : SelfJoinPoolShareResult

    /** Le reliquat du créateur n'est plus modifiable (déjà autorisé/capturé). */
    data object RemainderLocked : SelfJoinPoolShareResult

    /** Aucun reliquat créateur sur lequel prélever la part. */
    data object NoCreatorRemainder : SelfJoinPoolShareResult

    data object InvalidAmount : SelfJoinPoolShareResult

    /** Le montant demandé épuiserait (ou dépasserait) le reliquat du créateur : le dernier centime lui reste. */
    data object InsufficientRemainder : SelfJoinPoolShareResult
}

interface SelfJoinPoolShareUseCase {
    fun selfJoin(command: SelfJoinPoolShareCommand): SelfJoinPoolShareResult
}

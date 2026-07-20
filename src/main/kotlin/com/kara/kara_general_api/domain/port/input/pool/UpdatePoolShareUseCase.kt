package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal

/**
 * Le créateur modifie le montant d'une part non encore payée. L'écart est répercuté sur le reliquat du
 * créateur afin de préserver l'invariant (somme des parts == cible).
 */
data class UpdatePoolShareCommand(
    val poolId: PoolId,
    val shareId: PoolShareId,
    val requesterId: UserId,
    val newAmount: BigDecimal,
)

sealed interface UpdatePoolShareResult {
    data class Updated(val view: PoolView) : UpdatePoolShareResult

    data object PoolNotFound : UpdatePoolShareResult

    data object NotOwner : UpdatePoolShareResult

    data object ShareNotFound : UpdatePoolShareResult

    /** La cagnotte n'est plus ouverte. */
    data object PoolClosed : UpdatePoolShareResult

    /** La part visée est déjà autorisée ou capturée : elle ne peut plus être modifiée. */
    data object ShareAlreadyPaid : UpdatePoolShareResult

    /** On ne peut pas rééquilibrer le reliquat du créateur contre lui-même. */
    data object CannotEditCreatorShare : UpdatePoolShareResult

    /** Le reliquat du créateur est déjà autorisé/capturé, le rééquilibrage est impossible. */
    data object CreatorShareLocked : UpdatePoolShareResult

    /** Le rééquilibrage rendrait le reliquat du créateur nul ou négatif. */
    data object InsufficientRemainder : UpdatePoolShareResult

    data object InvalidAmount : UpdatePoolShareResult
}

interface UpdatePoolShareUseCase {
    fun updateShare(command: UpdatePoolShareCommand): UpdatePoolShareResult
}

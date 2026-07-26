package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal

/**
 * Le créateur ajoute un participant par email. Pour préserver l'invariant (somme des parts == cible), le
 * montant de la nouvelle part est prélevé sur le reliquat du créateur (part `isCreatorShare`).
 */
data class AddPoolShareCommand(
    val poolId: PoolId,
    val requesterId: UserId,
    val participantName: String,
    val email: String,
    val amount: BigDecimal,
)

sealed interface AddPoolShareResult {
    data class Added(
        val view: PoolView,
    ) : AddPoolShareResult

    data object PoolNotFound : AddPoolShareResult

    data object NotOwner : AddPoolShareResult

    /** La cagnotte n'est plus ouverte. */
    data object PoolClosed : AddPoolShareResult

    /** Aucun reliquat créateur (modifiable) sur lequel prélever la nouvelle part. */
    data object NoCreatorRemainder : AddPoolShareResult

    /** Le reliquat du créateur est insuffisant (ou déjà autorisé/capturé) pour financer la nouvelle part. */
    data object InsufficientRemainder : AddPoolShareResult

    data object InvalidShare : AddPoolShareResult
}

interface AddPoolShareUseCase {
    fun addShare(command: AddPoolShareCommand): AddPoolShareResult
}

package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId

data class RemindPoolShareCommand(
    val poolId: PoolId,
    val shareId: PoolShareId,
    val requesterId: UserId,
)

sealed interface RemindPoolShareResult {
    data object Reminded : RemindPoolShareResult

    data object PoolNotFound : RemindPoolShareResult

    data object NotOwner : RemindPoolShareResult

    data object ShareNotFound : RemindPoolShareResult

    /** La part n'a pas d'email connu : impossible d'envoyer une relance. */
    data object NoEmail : RemindPoolShareResult

    /** La part est déjà réglée (autorisée ou capturée) : aucune relance nécessaire. */
    data object AlreadyPaid : RemindPoolShareResult
}

interface RemindPoolShareUseCase {
    fun remind(command: RemindPoolShareCommand): RemindPoolShareResult
}

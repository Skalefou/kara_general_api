package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.user.UserId

data class RegeneratePoolLinkCommand(
    val poolId: PoolId,
    val requesterId: UserId,
)

sealed interface RegeneratePoolLinkResult {
    data class Regenerated(
        val globalLinkToken: String,
    ) : RegeneratePoolLinkResult

    data object PoolNotFound : RegeneratePoolLinkResult

    data object NotOwner : RegeneratePoolLinkResult

    data object PoolClosed : RegeneratePoolLinkResult
}

interface RegeneratePoolLinkUseCase {
    fun regenerate(command: RegeneratePoolLinkCommand): RegeneratePoolLinkResult
}

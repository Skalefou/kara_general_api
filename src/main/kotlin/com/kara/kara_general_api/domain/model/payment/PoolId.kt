package com.kara.kara_general_api.domain.model.payment

import java.util.UUID

@JvmInline
value class PoolId(val value: UUID) {
    companion object {
        fun generate(): PoolId = PoolId(UUID.randomUUID())
    }
}

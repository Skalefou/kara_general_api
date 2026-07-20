package com.kara.kara_general_api.domain.model.payment

import java.util.UUID

@JvmInline
value class PoolShareId(val value: UUID) {
    companion object {
        fun generate(): PoolShareId = PoolShareId(UUID.randomUUID())
    }
}

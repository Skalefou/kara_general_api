package com.kara.kara_general_api.domain.model.order

import java.util.UUID

@JvmInline
value class OrderId(val value: UUID) {
    companion object {
        fun generate(): OrderId = OrderId(UUID.randomUUID())
    }
}

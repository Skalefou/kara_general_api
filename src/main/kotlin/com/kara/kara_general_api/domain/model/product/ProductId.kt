package com.kara.kara_general_api.domain.model.product

import java.util.UUID

@JvmInline
value class ProductId(val value: UUID) {
    companion object {
        fun generate(): ProductId = ProductId(UUID.randomUUID())
    }
}

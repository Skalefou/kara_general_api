package com.kara.kara_general_api.domain.model.payment

import java.util.UUID

@JvmInline
value class PaymentId(
    val value: UUID,
) {
    companion object {
        fun generate(): PaymentId = PaymentId(UUID.randomUUID())
    }
}

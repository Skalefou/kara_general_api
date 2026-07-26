package com.kara.kara_general_api.domain.model.booking

import java.util.UUID

@JvmInline
value class BookingExtensionId(
    val value: UUID,
) {
    companion object {
        fun generate(): BookingExtensionId = BookingExtensionId(UUID.randomUUID())
    }
}

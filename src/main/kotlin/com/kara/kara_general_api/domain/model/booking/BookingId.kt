package com.kara.kara_general_api.domain.model.booking

import java.util.UUID

@JvmInline
value class BookingId(
    val value: UUID,
) {
    companion object {
        fun generate(): BookingId = BookingId(UUID.randomUUID())
    }
}

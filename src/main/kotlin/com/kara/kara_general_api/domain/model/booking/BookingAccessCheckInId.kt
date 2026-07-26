package com.kara.kara_general_api.domain.model.booking

import java.util.UUID

@JvmInline
value class BookingAccessCheckInId(val value: UUID) {
    companion object {
        fun generate(): BookingAccessCheckInId = BookingAccessCheckInId(UUID.randomUUID())
    }
}

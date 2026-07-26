package com.kara.kara_general_api.domain.model.room

import java.util.UUID

@JvmInline
value class RoomOptionId(
    val value: UUID,
) {
    companion object {
        fun generate(): RoomOptionId = RoomOptionId(UUID.randomUUID())
    }
}

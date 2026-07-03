package com.kara.kara_general_api.domain.model.room

import java.util.UUID

@JvmInline
value class RoomId(val value: UUID) {
    companion object {
        fun generate(): RoomId = RoomId(UUID.randomUUID())
    }
}

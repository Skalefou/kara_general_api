package com.kara.kara_general_api.domain.model.servershift

import java.util.UUID

@JvmInline
value class ServerShiftId(
    val value: UUID,
) {
    companion object {
        fun generate(): ServerShiftId = ServerShiftId(UUID.randomUUID())
    }
}

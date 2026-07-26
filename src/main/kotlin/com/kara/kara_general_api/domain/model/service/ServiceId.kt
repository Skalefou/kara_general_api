package com.kara.kara_general_api.domain.model.service

import java.util.UUID

@JvmInline
value class ServiceId(
    val value: UUID,
) {
    companion object {
        fun generate(): ServiceId = ServiceId(UUID.randomUUID())
    }
}

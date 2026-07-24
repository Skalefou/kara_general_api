package com.kara.kara_general_api.domain.port.input.booking

import java.time.Instant

interface CancelExpiredExtensionsUseCase {
    fun cancelExpired(now: Instant): Int
}

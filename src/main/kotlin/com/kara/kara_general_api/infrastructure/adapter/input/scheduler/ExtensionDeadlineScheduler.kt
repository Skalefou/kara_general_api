package com.kara.kara_general_api.infrastructure.adapter.input.scheduler

import com.kara.kara_general_api.domain.port.input.booking.CancelExpiredExtensionsUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ExtensionDeadlineScheduler(
    private val cancelExpiredExtensionsUseCase: CancelExpiredExtensionsUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun cancelExpiredExtensions() {
        val expired = cancelExpiredExtensionsUseCase.cancelExpired(Instant.now())
        if (expired > 0) {
            logger.info("Expired {} unpaid booking extension(s)", expired)
        }
    }
}

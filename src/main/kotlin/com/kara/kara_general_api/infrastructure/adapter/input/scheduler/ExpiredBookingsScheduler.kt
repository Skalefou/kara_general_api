package com.kara.kara_general_api.infrastructure.adapter.input.scheduler

import com.kara.kara_general_api.domain.port.input.booking.CancelExpiredBookingsUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Adaptateur primaire pilotant : annule périodiquement les réservations PENDING dont la fenêtre de
 * paiement (15 min) est échue, libérant leur créneau.
 */
@Component
class ExpiredBookingsScheduler(
    private val cancelExpiredBookingsUseCase: CancelExpiredBookingsUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun cancelExpiredBookings() {
        val cancelled = cancelExpiredBookingsUseCase.cancelExpired(Instant.now())
        if (cancelled > 0) {
            logger.info("Cancelled {} expired pending booking(s)", cancelled)
        }
    }
}

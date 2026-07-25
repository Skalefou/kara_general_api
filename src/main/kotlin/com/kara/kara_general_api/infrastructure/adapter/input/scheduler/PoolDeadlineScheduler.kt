package com.kara.kara_general_api.infrastructure.adapter.input.scheduler

import com.kara.kara_general_api.domain.port.input.pool.CancelExpiredPoolsUseCase
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Adaptateur primaire pilotant : balaie périodiquement les cagnottes OPEN dont le délai est échu et, pour
 * chacune, annule toutes les autorisations Stripe (zéro prélèvement) puis annule la réservation associée.
 */
@Component
class PoolDeadlineScheduler(
    private val cancelExpiredPoolsUseCase: CancelExpiredPoolsUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "cancelExpiredPools", lockAtMostFor = "PT5M", lockAtLeastFor = "PT5S")
    fun cancelExpiredPools() {
        val expired = cancelExpiredPoolsUseCase.cancelExpired(Instant.now())
        if (expired > 0) {
            logger.info("Expired {} incomplete pool(s); authorizations cancelled", expired)
        }
    }
}

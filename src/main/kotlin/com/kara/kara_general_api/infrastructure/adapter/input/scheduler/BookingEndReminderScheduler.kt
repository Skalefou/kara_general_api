package com.kara.kara_general_api.infrastructure.adapter.input.scheduler

import com.kara.kara_general_api.domain.port.input.notification.SendBookingEndRemindersUseCase
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Adaptateur primaire pilotant : envoie périodiquement les rappels de fin de réservation dus
 * (10 min et 2 min avant la fin du créneau).
 */
@Component
class BookingEndReminderScheduler(
    private val sendBookingEndRemindersUseCase: SendBookingEndRemindersUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "bookingEndReminders", lockAtMostFor = "PT5M", lockAtLeastFor = "PT5S")
    fun sendReminders() {
        val sent = sendBookingEndRemindersUseCase.sendDueReminders(Instant.now())
        if (sent > 0) {
            logger.info("Sent {} booking end reminder(s)", sent)
        }
    }
}

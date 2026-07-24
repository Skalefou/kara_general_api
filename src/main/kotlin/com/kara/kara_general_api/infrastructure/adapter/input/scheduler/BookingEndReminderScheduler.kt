package com.kara.kara_general_api.infrastructure.adapter.input.scheduler

import com.kara.kara_general_api.domain.port.input.notification.SendBookingEndRemindersUseCase
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
    fun sendReminders() {
        val sent = sendBookingEndRemindersUseCase.sendDueReminders(Instant.now())
        if (sent > 0) {
            logger.info("Sent {} booking end reminder(s)", sent)
        }
    }
}

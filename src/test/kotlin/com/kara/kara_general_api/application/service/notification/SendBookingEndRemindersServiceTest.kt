package com.kara.kara_general_api.application.service.notification

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderKind
import com.kara.kara_general_api.domain.model.notification.BookingEndReminderTarget
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.BookingEndReminderRepository
import com.kara.kara_general_api.domain.port.output.NotificationService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class SendBookingEndRemindersServiceTest {

    private val reminderRepository = mockk<BookingEndReminderRepository>()
    private val notificationService = mockk<NotificationService>(relaxed = true)
    private val sut = SendBookingEndRemindersService(reminderRepository, notificationService)

    private val now = Instant.parse("2026-08-01T20:50:00Z")
    private val bookingId = BookingId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())

    private fun target(
        token: String? = "device-token",
        roomName: String = "Salle Bleue",
        endAt: Instant = now.plusSeconds(300),
    ) = BookingEndReminderTarget(
        bookingId = bookingId,
        userId = userId,
        fcmToken = token,
        roomName = roomName,
        endAt = endAt,
    )

    private fun stubNoTwoMinutes() {
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TWO_MINUTES, now, now.plus(BookingEndReminderKind.TWO_MINUTES.lead))
        } returns emptyList()
    }

    private fun stubNoTenMinutes() {
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))
        } returns emptyList()
    }

    @Test
    fun `should send the ten minute reminder and mark it sent when due with a token`() {
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))
        } returns listOf(target())
        stubNoTwoMinutes()
        every { reminderRepository.markSent(bookingId, BookingEndReminderKind.TEN_MINUTES) } just Runs

        val sent = sut.sendDueReminders(now)

        assertEquals(1, sent)
        verify(exactly = 1) {
            notificationService.sendPushNotification(
                "device-token",
                "Votre réservation se termine bientôt",
                "Il vous reste environ 10 minutes dans Salle Bleue. Pensez à vous préparer à partir.",
                mapOf(
                    "bookingId" to bookingId.value.toString(),
                    "kind" to "TEN_MINUTES",
                    "minutesBefore" to "10",
                ),
            )
        }
        verify(exactly = 1) { reminderRepository.markSent(bookingId, BookingEndReminderKind.TEN_MINUTES) }
    }

    @Test
    fun `should send the two minute reminder and mark it sent when due with a token`() {
        stubNoTenMinutes()
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TWO_MINUTES, now, now.plus(BookingEndReminderKind.TWO_MINUTES.lead))
        } returns listOf(target(endAt = now.plusSeconds(60)))
        every { reminderRepository.markSent(bookingId, BookingEndReminderKind.TWO_MINUTES) } just Runs

        val sent = sut.sendDueReminders(now)

        assertEquals(1, sent)
        verify(exactly = 1) {
            notificationService.sendPushNotification(
                "device-token",
                "Fin imminente",
                "Il vous reste environ 2 minutes dans Salle Bleue.",
                mapOf(
                    "bookingId" to bookingId.value.toString(),
                    "kind" to "TWO_MINUTES",
                    "minutesBefore" to "2",
                ),
            )
        }
        verify(exactly = 1) { reminderRepository.markSent(bookingId, BookingEndReminderKind.TWO_MINUTES) }
    }

    @Test
    fun `should neither notify nor mark a target without a token`() {
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))
        } returns listOf(target(token = null))
        stubNoTwoMinutes()

        val sent = sut.sendDueReminders(now)

        assertEquals(0, sent)
        verify(exactly = 0) { notificationService.sendPushNotification(any(), any(), any(), any()) }
        verify(exactly = 0) { reminderRepository.markSent(any(), any()) }
    }

    @Test
    fun `should return the total number of notifications sent across both kinds`() {
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))
        } returns listOf(target())
        every {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TWO_MINUTES, now, now.plus(BookingEndReminderKind.TWO_MINUTES.lead))
        } returns listOf(target(endAt = now.plusSeconds(60)))
        every { reminderRepository.markSent(any(), any()) } just Runs

        val sent = sut.sendDueReminders(now)

        assertEquals(2, sent)
    }

    @Test
    fun `should query both kinds with the from now and to now plus lead windows`() {
        stubNoTenMinutes()
        stubNoTwoMinutes()

        sut.sendDueReminders(now)

        verify(exactly = 1) {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TEN_MINUTES, now, now.plus(BookingEndReminderKind.TEN_MINUTES.lead))
        }
        verify(exactly = 1) {
            reminderRepository.findConfirmedDue(BookingEndReminderKind.TWO_MINUTES, now, now.plus(BookingEndReminderKind.TWO_MINUTES.lead))
        }
    }
}

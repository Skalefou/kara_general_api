package com.kara.kara_general_api.domain.model.notification

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class BookingEndReminderKindTest {

    @Test
    fun `TEN_MINUTES carries a 10 minute lead`() {
        assertEquals(Duration.ofMinutes(10), BookingEndReminderKind.TEN_MINUTES.lead)
        assertEquals(10, BookingEndReminderKind.TEN_MINUTES.minutesBefore)
    }

    @Test
    fun `TWO_MINUTES carries a 2 minute lead`() {
        assertEquals(Duration.ofMinutes(2), BookingEndReminderKind.TWO_MINUTES.lead)
        assertEquals(2, BookingEndReminderKind.TWO_MINUTES.minutesBefore)
    }
}

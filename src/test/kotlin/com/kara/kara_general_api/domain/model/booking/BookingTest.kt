package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class BookingTest {
    @Test
    fun `create sets a 15-minute payment window from createdAt`() {
        val booking =
            Booking.create(
                roomId = RoomId(UUID.randomUUID()),
                userId = UserId(UUID.randomUUID()),
                startAt = Instant.parse("2026-08-01T18:00:00Z"),
                endAt = Instant.parse("2026-08-01T21:00:00Z"),
                numberOfPeople = 8,
                selectedOptionIds = emptyList(),
                totalPrice = BigDecimal("435.00"),
                currency = Currency.EUR,
            )

        assertEquals(booking.createdAt.plus(Duration.ofMinutes(15)), booking.expiresAt)
        assertEquals(BookingStatus.PENDING, booking.status)
    }
}

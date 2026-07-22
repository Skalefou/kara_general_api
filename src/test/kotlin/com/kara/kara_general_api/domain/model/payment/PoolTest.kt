package com.kara.kara_general_api.domain.model.payment

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.Currency
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoolTest {

    private fun pool(deadline: Instant = Instant.now().plusSeconds(3600)) =
        Pool.create(
            bookingId = BookingId(UUID.randomUUID()),
            targetAmount = BigDecimal("100.00"),
            currency = Currency.EUR,
            deadline = deadline,
            globalLinkToken = "tok",
        )

    @Test
    fun `defaultDeadline caps at 24h when the reservation is far away`() {
        val now = Instant.parse("2026-08-01T00:00:00Z")
        val reservationStart = now.plus(Duration.ofDays(10))

        val deadline = Pool.defaultDeadline(now, reservationStart)

        assertEquals(now.plus(Duration.ofHours(24)), deadline)
    }

    @Test
    fun `defaultDeadline falls back to two hours before the reservation when it is close`() {
        val now = Instant.parse("2026-08-01T00:00:00Z")
        val reservationStart = now.plus(Duration.ofHours(10))

        val deadline = Pool.defaultDeadline(now, reservationStart)

        assertEquals(reservationStart.minus(Duration.ofHours(2)), deadline)
    }

    @Test
    fun `defaultDeadline is always strictly under seven days`() {
        val now = Instant.parse("2026-08-01T00:00:00Z")
        val deadline = Pool.defaultDeadline(now, now.plus(Duration.ofDays(30)))

        assertTrue(deadline.isBefore(now.plus(Duration.ofDays(7))))
    }

    @Test
    fun `isExpired is true once the deadline is reached`() {
        val p = pool(deadline = Instant.now().minusSeconds(1))

        assertTrue(p.isExpired(Instant.now()))
    }

    @Test
    fun `isExpired is false before the deadline`() {
        assertFalse(pool(deadline = Instant.now().plusSeconds(3600)).isExpired(Instant.now()))
    }

    @Test
    fun `status transitions preserve the frozen target amount`() {
        val p = pool()

        assertEquals(PoolStatus.SETTLED, p.markSettled().status)
        assertEquals(PoolStatus.EXPIRED, p.markExpired().status)
        assertEquals(PoolStatus.CANCELLED, p.markCancelled().status)
        assertEquals(BigDecimal("100.00"), p.markSettled().targetAmount)
    }
}

package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TicketCodeTest {
    private fun booking(
        id: BookingId,
        status: BookingStatus,
    ) = Booking(
        id = id,
        roomId = RoomId(UUID.randomUUID()),
        userId = UserId(UUID.randomUUID()),
        startAt = Instant.now().plusSeconds(3600),
        endAt = Instant.now().plusSeconds(7200),
        numberOfPeople = 4,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("100.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(900),
    )

    @Test
    fun `is deterministic for a given booking id`() {
        val id = BookingId(UUID.randomUUID())

        assertEquals(
            booking(id, BookingStatus.CONFIRMED).ticketCode(),
            booking(id, BookingStatus.PENDING).ticketCode(),
        )
    }

    @Test
    fun `has the KARA-TKT prefix and 8 Crockford base32 characters`() {
        val code = booking(BookingId(UUID.randomUUID()), BookingStatus.CONFIRMED).ticketCode()

        assertTrue(code.startsWith("KARA-TKT-"), "unexpected code: $code")
        val suffix = code.removePrefix("KARA-TKT-")
        assertEquals(8, suffix.length)
        assertTrue(suffix.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZ" }, "unexpected suffix: $suffix")
    }

    @Test
    fun `ticketCodeOrNull is present only when confirmed`() {
        val id = BookingId(UUID.randomUUID())

        assertNotNull(booking(id, BookingStatus.CONFIRMED).ticketCodeOrNull())
        assertNull(booking(id, BookingStatus.PENDING).ticketCodeOrNull())
        assertNull(booking(id, BookingStatus.CANCELLED).ticketCodeOrNull())
    }
}

package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookingExtensionPlannerTest {

    private val endAt: Instant = Instant.parse("2026-07-24T20:00:00Z")

    private fun room(
        pricePerPersonPerHour: BigDecimal = BigDecimal("10.00"),
        opensAt: LocalTime? = null,
        closesAt: LocalTime? = null,
    ) = Room(
        id = RoomId.generate(),
        name = "Salle",
        description = "",
        address = Address("1 rue", "Paris", "75001", "France"),
        pricePerPersonPerHour = pricePerPersonPerHour,
        currency = Currency.EUR,
        maxCapacity = 50,
        isThereWifi = true,
        isThereSonoPro = false,
        isThereAirConditioning = false,
        createdAt = Instant.now(),
        opensAt = opensAt,
        closesAt = closesAt,
        timeZone = ZoneId.of("UTC"),
    )

    private fun booking(numberOfPeople: Int = 4) =
        Booking(
            id = BookingId.generate(),
            roomId = RoomId.generate(),
            userId = UserId(UUID.randomUUID()),
            startAt = endAt.minus(Duration.ofHours(2)),
            endAt = endAt,
            numberOfPeople = numberOfPeople,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("80.00"),
            currency = Currency.EUR,
            status = BookingStatus.CONFIRMED,
            createdAt = Instant.now(),
            expiresAt = Instant.now(),
        )

    @Test
    fun `should cap the extension at the offered maximum when nothing blocks the slot`() {
        val max = BookingExtensionPlanner.maxAdditionalMinutes(booking(), null, null)

        assertEquals(120, max)
    }

    @Test
    fun `should floor the extension to a 30 minute step when the next booking is close`() {
        val nextBooking = endAt.plus(Duration.ofMinutes(80))

        val max = BookingExtensionPlanner.maxAdditionalMinutes(booking(), nextBooking, null)

        assertEquals(60, max)
    }

    @Test
    fun `should return zero when the room closes right at the end of the booking`() {
        val max = BookingExtensionPlanner.maxAdditionalMinutes(booking(), null, endAt)

        assertEquals(0, max)
    }

    @Test
    fun `should keep the earliest blocker between the next booking and the closing time`() {
        val nextBooking = endAt.plus(Duration.ofHours(2))
        val closing = endAt.plus(Duration.ofMinutes(45))

        val max = BookingExtensionPlanner.maxAdditionalMinutes(booking(), nextBooking, closing)

        assertEquals(30, max)
    }

    @Test
    fun `should price the extension per person and per hour`() {
        val price = BookingExtensionPlanner.price(room(), booking(numberOfPeople = 4), 90)

        assertEquals(BigDecimal("60.00"), price)
    }

    @Test
    fun `should reject a duration that is not a multiple of the step`() {
        assertFalse(BookingExtensionPlanner.isValidDuration(45, 120))
    }

    @Test
    fun `should reject a duration above the available maximum`() {
        assertFalse(BookingExtensionPlanner.isValidDuration(90, 60))
    }

    @Test
    fun `should accept a valid duration within the maximum`() {
        assertTrue(BookingExtensionPlanner.isValidDuration(60, 120))
    }

    @Test
    fun `should not expose any closing time when the room is open around the clock`() {
        assertNull(room().nextClosingAfter(endAt))
    }

    @Test
    fun `should return the same day closing time when it is still ahead`() {
        val room = room(opensAt = LocalTime.of(9, 0), closesAt = LocalTime.of(23, 0))

        assertEquals(Instant.parse("2026-07-24T23:00:00Z"), room.nextClosingAfter(endAt))
    }

    @Test
    fun `should roll over to the next day when the closing time is already passed`() {
        val room = room(opensAt = LocalTime.of(9, 0), closesAt = LocalTime.of(18, 0))

        assertEquals(Instant.parse("2026-07-25T18:00:00Z"), room.nextClosingAfter(endAt))
    }
}

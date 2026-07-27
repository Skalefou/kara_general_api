package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookingEstimatorTest {
    private val roomId = RoomId(UUID.randomUUID())

    private fun room(
        pricePerPersonPerHour: String = "12.50",
        maxCapacity: Int = 50,
    ): Room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            description = "Grande salle lumineuse",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            pricePerPersonPerHour = BigDecimal(pricePerPersonPerHour),
            currency = Currency.EUR,
            maxCapacity = maxCapacity,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
            createdAt = Instant.now(),
        )

    private fun option(
        id: RoomOptionId,
        price: String,
    ): RoomOption =
        RoomOption(
            id = id,
            roomId = roomId,
            label = "Option $price",
            description = null,
            price = BigDecimal(price),
            currency = Currency.EUR,
        )

    private val start = Instant.parse("2026-08-01T18:00:00Z")

    @Test
    fun `should compute base, options and per-person price for a nominal request`() {
        // 12.50 * 8 personnes * 3,5 h = 350.00 ; options 60 + 25 = 85.00 ; total 435.00 ; 435/8 = 54.375 -> 54.38
        val cleaning = RoomOptionId(UUID.randomUUID())
        val security = RoomOptionId(UUID.randomUUID())
        val options = listOf(option(cleaning, "60.00"), option(security, "25.00"))

        val result =
            BookingEstimator.estimate(
                room = room(),
                roomOptions = options,
                numberOfPeople = 8,
                startAt = start,
                endAt = start.plusSeconds(3 * 3600 + 1800),
                requestedOptionIds = listOf(cleaning, security),
            )

        val success = assertIs<EstimateBookingResult.Success>(result)
        assertEquals(BigDecimal("350.00"), success.estimate.base)
        assertEquals(BigDecimal("85.00"), success.estimate.optionsTotal)
        assertEquals(BigDecimal("435.00"), success.estimate.totalPrice)
        assertEquals(BigDecimal("54.38"), success.estimate.pricePerPerson)
        assertEquals(Currency.EUR, success.estimate.currency)
    }

    @Test
    fun `should round fractional hours and per-person price HALF_UP`() {
        // 100 minutes = 1,6667 h ; 10.00 * 3 * 1,6667 = 50.001 -> 50.00 ; 50.00/3 = 16.6667 -> 16.67
        val result =
            BookingEstimator.estimate(
                room = room(pricePerPersonPerHour = "10.00"),
                roomOptions = emptyList(),
                numberOfPeople = 3,
                startAt = start,
                endAt = start.plusSeconds(100 * 60),
                requestedOptionIds = emptyList(),
            )

        val success = assertIs<EstimateBookingResult.Success>(result)
        assertEquals(BigDecimal("50.00"), success.estimate.base)
        assertEquals(BigDecimal("0.00"), success.estimate.optionsTotal)
        assertEquals(BigDecimal("50.00"), success.estimate.totalPrice)
        assertEquals(BigDecimal("16.67"), success.estimate.pricePerPerson)
    }

    @Test
    fun `should reject fewer than two people`() {
        val result =
            BookingEstimator.estimate(
                room = room(),
                roomOptions = emptyList(),
                numberOfPeople = 1,
                startAt = start,
                endAt = start.plusSeconds(3600),
                requestedOptionIds = emptyList(),
            )

        assertEquals(EstimateBookingResult.TooFewPeople, result)
    }

    @Test
    fun `should reject when number of people exceeds the room capacity`() {
        val result =
            BookingEstimator.estimate(
                room = room(maxCapacity = 10),
                roomOptions = emptyList(),
                numberOfPeople = 11,
                startAt = start,
                endAt = start.plusSeconds(3600),
                requestedOptionIds = emptyList(),
            )

        assertEquals(EstimateBookingResult.CapacityExceeded(10), result)
    }

    @Test
    fun `should reject when end is not after start`() {
        val result =
            BookingEstimator.estimate(
                room = room(),
                roomOptions = emptyList(),
                numberOfPeople = 4,
                startAt = start,
                endAt = start,
                requestedOptionIds = emptyList(),
            )

        assertEquals(EstimateBookingResult.InvalidTimeSlot, result)
    }

    @Test
    fun `should reject a slot shorter than the minimum duration`() {
        val result =
            BookingEstimator.estimate(
                room = room(),
                roomOptions = emptyList(),
                numberOfPeople = 4,
                startAt = start,
                endAt = start.plusSeconds(30 * 60),
                requestedOptionIds = emptyList(),
            )

        assertEquals(EstimateBookingResult.DurationTooShort(60), result)
    }

    @Test
    fun `should accept a slot of exactly the minimum duration`() {
        val result =
            BookingEstimator.estimate(
                room = room(pricePerPersonPerHour = "10.00"),
                roomOptions = emptyList(),
                numberOfPeople = 4,
                startAt = start,
                endAt = start.plus(Booking.MIN_DURATION),
                requestedOptionIds = emptyList(),
            )

        val success = assertIs<EstimateBookingResult.Success>(result)
        assertEquals(BigDecimal("40.00"), success.estimate.totalPrice)
    }

    @Test
    fun `should keep InvalidTimeSlot when the interval is reversed`() {
        val result =
            BookingEstimator.estimate(
                room = room(),
                roomOptions = emptyList(),
                numberOfPeople = 4,
                startAt = start,
                endAt = start.minusSeconds(30 * 60),
                requestedOptionIds = emptyList(),
            )

        assertEquals(EstimateBookingResult.InvalidTimeSlot, result)
    }

    @Test
    fun `should reject an option that does not belong to the room`() {
        val known = RoomOptionId(UUID.randomUUID())
        val foreign = RoomOptionId(UUID.randomUUID())

        val result =
            BookingEstimator.estimate(
                room = room(),
                roomOptions = listOf(option(known, "60.00")),
                numberOfPeople = 4,
                startAt = start,
                endAt = start.plusSeconds(3600),
                requestedOptionIds = listOf(known, foreign),
            )

        val unknown = assertIs<EstimateBookingResult.UnknownOptions>(result)
        assertEquals(listOf(foreign.value), unknown.optionIds)
    }
}

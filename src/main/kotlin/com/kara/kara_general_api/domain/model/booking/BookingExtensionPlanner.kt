package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Room
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

object BookingExtensionPlanner {
    val STEP_MINUTES: Int = 30

    val OFFERED_STEPS: List<Int> = listOf(30, 60, 90, 120)

    fun maxAdditionalMinutes(
        booking: Booking,
        nextBookingStart: Instant?,
        nextClosing: Instant?,
    ): Int {
        val hardLimit = listOfNotNull(nextBookingStart, nextClosing).minOrNull() ?: return OFFERED_STEPS.last()
        val available = Duration.between(booking.endAt, hardLimit).toMinutes()
        if (available <= 0) return 0
        val capped = minOf(available, OFFERED_STEPS.last().toLong())
        return (capped / STEP_MINUTES * STEP_MINUTES).toInt()
    }

    fun price(
        room: Room,
        booking: Booking,
        additionalMinutes: Int,
    ): BigDecimal {
        val hours = BigDecimal(additionalMinutes).divide(BigDecimal(60), 4, RoundingMode.HALF_UP)
        return room.pricePerPersonPerHour
            .multiply(BigDecimal(booking.numberOfPeople))
            .multiply(hours)
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun isValidDuration(
        additionalMinutes: Int,
        maxAdditionalMinutes: Int,
    ): Boolean =
        additionalMinutes >= BookingExtension.MIN_ADDITIONAL_MINUTES &&
            additionalMinutes % STEP_MINUTES == 0 &&
            additionalMinutes <= maxAdditionalMinutes
}

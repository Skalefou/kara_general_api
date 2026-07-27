package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/**
 * Calcul pur d'une estimation de réservation. Aucune dépendance externe, aucune persistance.
 *
 * Choix d'arrondi : la durée est exprimée en heures fractionnaires (minutes / 60) à l'échelle 4
 * en HALF_UP, afin de conserver les créneaux non ronds (ex. 90 min = 1,5000 h). Les montants
 * monétaires ([BookingEstimate.base], [BookingEstimate.optionsTotal], [BookingEstimate.totalPrice]
 * et [BookingEstimate.pricePerPerson]) sont ensuite arrondis à 2 décimales en HALF_UP.
 */
object BookingEstimator {
    fun estimate(
        room: Room,
        roomOptions: List<RoomOption>,
        numberOfPeople: Int,
        startAt: Instant,
        endAt: Instant,
        requestedOptionIds: List<RoomOptionId>,
    ): EstimateBookingResult {
        if (numberOfPeople < Room.MIN_CAPACITY) return EstimateBookingResult.TooFewPeople
        if (numberOfPeople > room.maxCapacity) return EstimateBookingResult.CapacityExceeded(room.maxCapacity)
        if (!endAt.isAfter(startAt)) return EstimateBookingResult.InvalidTimeSlot
        if (Duration.between(startAt, endAt) < Booking.MIN_DURATION) {
            return EstimateBookingResult.DurationTooShort(Booking.MIN_DURATION.toMinutes())
        }

        val optionsById = roomOptions.associateBy { it.id }
        val requested = requestedOptionIds.distinct()
        val unknown = requested.filterNot { optionsById.containsKey(it) }
        if (unknown.isNotEmpty()) return EstimateBookingResult.UnknownOptions(unknown.map { it.value })

        val minutes = Duration.between(startAt, endAt).toMinutes()
        val hours = BigDecimal(minutes).divide(BigDecimal(60), 4, RoundingMode.HALF_UP)

        val base =
            room.pricePerPersonPerHour
                .multiply(BigDecimal(numberOfPeople))
                .multiply(hours)
                .setScale(2, RoundingMode.HALF_UP)

        val optionsTotal =
            requested
                .fold(BigDecimal.ZERO) { acc, id -> acc.add(optionsById.getValue(id).price) }
                .setScale(2, RoundingMode.HALF_UP)

        val total = base.add(optionsTotal)
        val pricePerPerson = total.divide(BigDecimal(numberOfPeople), 2, RoundingMode.HALF_UP)

        return EstimateBookingResult.Success(
            BookingEstimate(
                totalPrice = total,
                pricePerPerson = pricePerPerson,
                currency = room.currency,
                base = base,
                optionsTotal = optionsTotal,
            ),
        )
    }
}

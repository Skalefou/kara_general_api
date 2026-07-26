package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingExtensionPlanner
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ExtensionFeasibility(
    private val bookingRepository: BookingRepository,
    private val bookingExtensionRepository: BookingExtensionRepository,
) {
    fun maxAdditionalMinutes(
        booking: Booking,
        room: Room,
        now: Instant,
    ): Int {
        val nextBookingStart =
            bookingRepository.findNextStartAfter(booking.roomId, booking.endAt, booking.id, now)
        val nextExtensionStart =
            bookingExtensionRepository.findNextHeldStart(booking.roomId, booking.endAt, booking.id, now)
        val nextOccupied = listOfNotNull(nextBookingStart, nextExtensionStart).minOrNull()
        val nextClosing = room.nextClosingAfter(booking.endAt)
        return BookingExtensionPlanner.maxAdditionalMinutes(booking, nextOccupied, nextClosing)
    }
}

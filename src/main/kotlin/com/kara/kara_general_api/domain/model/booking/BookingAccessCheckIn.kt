package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Duration
import java.time.Instant

data class BookingAccessCheckIn(
    val id: BookingAccessCheckInId,
    val bookingId: BookingId,
    val serverId: UserId,
    val checkedInAt: Instant,
) {
    companion object {
        val EARLY_ARRIVAL_TOLERANCE: Duration = Duration.ofMinutes(30)

        fun record(bookingId: BookingId, serverId: UserId, now: Instant): BookingAccessCheckIn =
            BookingAccessCheckIn(
                id = BookingAccessCheckInId.generate(),
                bookingId = bookingId,
                serverId = serverId,
                checkedInAt = now,
            )

        fun isWithinAdmissionWindow(booking: Booking, now: Instant): Boolean {
            val admissionOpensAt = booking.startAt.minus(EARLY_ARRIVAL_TOLERANCE)
            return !now.isBefore(admissionOpensAt) && now.isBefore(booking.endAt)
        }
    }
}

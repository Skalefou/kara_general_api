package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckIn
import com.kara.kara_general_api.domain.model.booking.BookingId

interface BookingAccessCheckInRepository {
    fun findByBookingId(bookingId: BookingId): BookingAccessCheckIn?

    fun recordIfAbsent(checkIn: BookingAccessCheckIn): BookingAccessCheckIn
}

package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.RoomId
import java.time.Instant

interface BookingExtensionRepository {
    fun save(extension: BookingExtension): BookingExtension

    fun findById(id: BookingExtensionId): BookingExtension?

    fun findPendingByBookingId(bookingId: BookingId): BookingExtension?

    fun updateStatus(id: BookingExtensionId, status: BookingExtensionStatus)

    fun findNextHeldStart(roomId: RoomId, after: Instant, excluding: BookingId, now: Instant): Instant?

    fun findExpiredPending(now: Instant): List<BookingExtension>
}

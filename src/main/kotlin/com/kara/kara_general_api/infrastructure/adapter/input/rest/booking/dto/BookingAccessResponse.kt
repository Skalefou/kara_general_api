package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.port.input.booking.BookingAccessView
import java.time.Instant

data class BookingAccessResponse(
    val bookingId: String,
    val ticketCode: String,
    val clientName: String,
    val roomName: String,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val status: String,
    val granted: Boolean,
    val checkedInAt: Instant?,
    val alreadyCheckedIn: Boolean,
    val checkedInByName: String?,
) {
    companion object {
        fun granted(
            view: BookingAccessView,
            checkedInAt: Instant,
        ): BookingAccessResponse = from(view).copy(granted = true, checkedInAt = checkedInAt)

        fun alreadyCheckedIn(
            view: BookingAccessView,
            firstCheckedInAt: Instant,
            checkedInByName: String?,
        ): BookingAccessResponse =
            from(view).copy(
                granted = false,
                checkedInAt = firstCheckedInAt,
                alreadyCheckedIn = true,
                checkedInByName = checkedInByName,
            )

        fun from(view: BookingAccessView): BookingAccessResponse =
            BookingAccessResponse(
                bookingId = view.bookingId.toString(),
                ticketCode = view.ticketCode,
                clientName = view.clientName,
                roomName = view.roomName,
                startAt = view.startAt,
                endAt = view.endAt,
                numberOfPeople = view.numberOfPeople,
                status = view.status.name,
                granted = false,
                checkedInAt = null,
                alreadyCheckedIn = false,
                checkedInByName = null,
            )
    }
}

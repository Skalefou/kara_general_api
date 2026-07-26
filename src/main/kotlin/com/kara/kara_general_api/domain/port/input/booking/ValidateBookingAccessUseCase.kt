package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant
import java.util.UUID

data class ValidateBookingAccessCommand(
    val bookingId: BookingId,
    val currentUserId: UserId,
    val isAdmin: Boolean,
)

data class BookingAccessView(
    val bookingId: UUID,
    val ticketCode: String,
    val clientName: String,
    val roomName: String,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val status: BookingStatus,
)

sealed interface ValidateBookingAccessResult {
    data class Granted(val view: BookingAccessView, val checkedInAt: Instant) : ValidateBookingAccessResult

    data class AlreadyCheckedIn(
        val view: BookingAccessView,
        val firstCheckedInAt: Instant,
        val checkedInByName: String?,
    ) : ValidateBookingAccessResult

    data object BookingNotFound : ValidateBookingAccessResult

    data object NotAssignedServer : ValidateBookingAccessResult

    data class NotConfirmed(val view: BookingAccessView) : ValidateBookingAccessResult

    data class OutsideAdmissionWindow(val view: BookingAccessView) : ValidateBookingAccessResult

    data object RoomNotFound : ValidateBookingAccessResult
}

interface ValidateBookingAccessUseCase {
    fun validate(command: ValidateBookingAccessCommand): ValidateBookingAccessResult
}

package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingAccessCheckIn
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.ticketCode
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.user.displayName
import com.kara.kara_general_api.domain.port.input.booking.BookingAccessView
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessCommand
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessResult
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessUseCase
import com.kara.kara_general_api.domain.port.output.BookingAccessCheckInRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private const val UNKNOWN_CLIENT = "Client inconnu"
private const val UNKNOWN_SERVER = "Serveur inconnu"

@Service
class ValidateBookingAccessService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val serverShiftRepository: ServerShiftRepository,
    private val bookingAccessCheckInRepository: BookingAccessCheckInRepository,
) : ValidateBookingAccessUseCase {
    @Transactional
    override fun validate(command: ValidateBookingAccessCommand): ValidateBookingAccessResult {
        val booking =
            bookingRepository.findById(command.bookingId)
                ?: return ValidateBookingAccessResult.BookingNotFound

        if (!command.isAdmin) {
            val assigned =
                serverShiftRepository.findServerIdsAssignedTo(booking.roomId, booking.startAt, booking.endAt)
            if (command.currentUserId !in assigned) return ValidateBookingAccessResult.NotAssignedServer
        }

        val room = roomRepository.findById(booking.roomId) ?: return ValidateBookingAccessResult.RoomNotFound
        val view = viewOf(booking, room)

        bookingAccessCheckInRepository.findByBookingId(booking.id)?.let { existing ->
            return alreadyCheckedIn(view, existing)
        }

        if (booking.status != BookingStatus.CONFIRMED) {
            return ValidateBookingAccessResult.NotConfirmed(view)
        }

        val now = Instant.now()
        if (!BookingAccessCheckIn.isWithinAdmissionWindow(booking, now)) {
            return ValidateBookingAccessResult.OutsideAdmissionWindow(view)
        }

        val attempted = BookingAccessCheckIn.record(booking.id, command.currentUserId, now)
        val effective = bookingAccessCheckInRepository.recordIfAbsent(attempted)
        return if (effective.id == attempted.id) {
            ValidateBookingAccessResult.Granted(view, effective.checkedInAt)
        } else {
            alreadyCheckedIn(view, effective)
        }
    }

    private fun alreadyCheckedIn(
        view: BookingAccessView,
        checkIn: BookingAccessCheckIn,
    ): ValidateBookingAccessResult.AlreadyCheckedIn =
        ValidateBookingAccessResult.AlreadyCheckedIn(
            view = view,
            firstCheckedInAt = checkIn.checkedInAt,
            checkedInByName = userRepository.findById(checkIn.serverId).displayName(UNKNOWN_SERVER),
        )

    private fun viewOf(
        booking: Booking,
        room: Room,
    ): BookingAccessView =
        BookingAccessView(
            bookingId = booking.id.value,
            ticketCode = booking.ticketCode(),
            clientName = userRepository.findById(booking.userId).displayName(UNKNOWN_CLIENT),
            roomName = room.name,
            startAt = booking.startAt,
            endAt = booking.endAt,
            numberOfPeople = booking.numberOfPeople,
            status = booking.status,
        )
}

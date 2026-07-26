package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.ticketCodeOrNull
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.BookingDetailView
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailResult
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Détail d'une réservation + billet, réservé au client propriétaire. Lecture seule. */
@Service
class GetBookingDetailService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
) : GetBookingDetailUseCase {
    @Transactional(readOnly = true)
    override fun getDetail(
        bookingId: BookingId,
        requesterId: UserId,
    ): GetBookingDetailResult {
        val booking = bookingRepository.findById(bookingId) ?: return GetBookingDetailResult.NotFound
        if (booking.userId != requesterId) return GetBookingDetailResult.NotOwner
        val room = roomRepository.findById(booking.roomId)
        return GetBookingDetailResult.Found(
            BookingDetailView(
                bookingId = booking.id.value,
                roomName = room?.name ?: "Salle",
                roomAddress = room?.let { formatAddress(it) },
                startAt = booking.startAt,
                endAt = booking.endAt,
                numberOfPeople = booking.numberOfPeople,
                totalPrice = booking.totalPrice,
                currency = booking.currency,
                status = booking.status,
                paymentMode = booking.paymentMode,
                ticketCode = booking.ticketCodeOrNull(),
            ),
        )
    }

    private fun formatAddress(room: Room): String = with(room.address) { "$street, $postalCode $city, $country" }
}

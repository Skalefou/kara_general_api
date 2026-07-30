package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.ticketCodeOrNull
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.BookingDetailView
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailResult
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Détail d'une réservation + billet, ouvert aux utilisateurs **impliqués** dans celle-ci : son organisateur
 * (propriétaire) et tout participant détenant une part de sa cagnotte — même sémantique d'implication que
 * « Mes événements ». Tout autre requérant reçoit [GetBookingDetailResult.NotOwner]. Lecture seule : le rôle
 * est renvoyé dans la vue (`isCreator`) pour que le front masque les actions réservées à l'organisateur.
 */
@Service
class GetBookingDetailService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val poolShareRepository: PoolShareRepository,
) : GetBookingDetailUseCase {
    @Transactional(readOnly = true)
    override fun getDetail(
        bookingId: BookingId,
        requesterId: UserId,
    ): GetBookingDetailResult {
        val booking = bookingRepository.findById(bookingId) ?: return GetBookingDetailResult.NotFound
        val isCreator = booking.userId == requesterId
        // L'appel de vérification des parts n'est émis que pour un non-propriétaire : le cas courant
        // (l'organisateur consulte son événement) reste à une seule requête de plus qu'avant.
        if (!isCreator && !poolShareRepository.existsForBookingAndPayer(bookingId, requesterId)) {
            return GetBookingDetailResult.NotOwner
        }
        val room = roomRepository.findById(booking.roomId)
        return GetBookingDetailResult.Found(
            BookingDetailView(
                bookingId = booking.id.value,
                roomName = room?.name ?: "Salle",
                roomAddress = room?.let { formatAddress(it) },
                roomLatitude = room?.latitude,
                roomLongitude = room?.longitude,
                startAt = booking.startAt,
                endAt = booking.endAt,
                numberOfPeople = booking.numberOfPeople,
                totalPrice = booking.totalPrice,
                currency = booking.currency,
                status = booking.status,
                paymentMode = booking.paymentMode,
                ticketCode = booking.ticketCodeOrNull(),
                isCreator = isCreator,
            ),
        )
    }

    private fun formatAddress(room: Room): String = with(room.address) { "$street, $postalCode $city, $country" }
}

package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.AdminBooking
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.ListAllBookingsUseCase
import com.kara.kara_general_api.domain.model.user.displayName
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

/**
 * Toutes les réservations enrichies (salle + client) pour la supervision admin. Salles et clients sont
 * résolus une seule fois chacun (caches locaux) pour éviter les requêtes redondantes.
 */
@Service
class ListAllBookingsService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
) : ListAllBookingsUseCase {

    override fun listAllBookings(): List<AdminBooking> {
        val roomCache = mutableMapOf<RoomId, Room?>()
        val userCache = mutableMapOf<UserId, User?>()
        return bookingRepository.findAllBookings().map { booking ->
            val room = roomCache.getOrPut(booking.roomId) { roomRepository.findById(booking.roomId) }
            val client = userCache.getOrPut(booking.userId) { userRepository.findById(booking.userId) }
            AdminBooking(
                booking = booking,
                roomName = room?.name ?: "Salle inconnue",
                clientName = client.displayName("Client inconnu"),
            )
        }
    }
}

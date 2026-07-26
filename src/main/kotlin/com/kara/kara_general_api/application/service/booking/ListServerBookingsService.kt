package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.ServerBooking
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.ListServerBookingsUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service

/**
 * Réservations rattachées à un serveur (via le chevauchement avec son agenda), enrichies du nom de la
 * salle. Les salles sont résolues une seule fois chacune (cache local).
 */
@Service
class ListServerBookingsService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
) : ListServerBookingsUseCase {
    override fun listServerBookings(serverId: UserId): List<ServerBooking> {
        val bookings = bookingRepository.findAssignedToServer(serverId)
        val roomCache = mutableMapOf<RoomId, Room?>()
        return bookings.map { booking ->
            val room = roomCache.getOrPut(booking.roomId) { roomRepository.findById(booking.roomId) }
            ServerBooking(booking = booking, roomName = room?.name ?: "Salle inconnue")
        }
    }
}

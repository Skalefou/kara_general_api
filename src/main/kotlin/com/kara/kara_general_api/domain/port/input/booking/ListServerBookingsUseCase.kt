package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.ServerBooking
import com.kara.kara_general_api.domain.model.user.UserId

/**
 * Réservations dont un serveur est rattaché : celles dont la salle et le créneau chevauchent l'un de ses
 * créneaux d'agenda (server_shifts). Les réservations annulées sont exclues. Ordonnées par date de début.
 */
interface ListServerBookingsUseCase {
    fun listServerBookings(serverId: UserId): List<ServerBooking>
}

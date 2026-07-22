package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.AdminBooking

/**
 * Supervision admin : toutes les réservations de la plateforme, enrichies du nom de la salle et du
 * nom du client, ordonnées par date de début décroissante.
 */
interface ListAllBookingsUseCase {
    fun listAllBookings(): List<AdminBooking>
}

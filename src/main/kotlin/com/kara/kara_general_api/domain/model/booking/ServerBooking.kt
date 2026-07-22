package com.kara.kara_general_api.domain.model.booking

/**
 * Vue d'une réservation destinée au serveur qui y est rattaché (via son agenda). Récap essentiel :
 * la réservation plus le nom de la salle. N'expose aucune donnée personnelle du client.
 */
data class ServerBooking(
    val booking: Booking,
    val roomName: String,
)

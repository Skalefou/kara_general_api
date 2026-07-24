package com.kara.kara_general_api.domain.model.booking

/**
 * Vue d'une réservation pour la supervision admin : la réservation plus le nom de la salle et le nom
 * du client. L'admin a un accès complet ; l'identité du client est donc exposée ici.
 */
data class AdminBooking(
    val booking: Booking,
    val roomName: String,
    val clientName: String,
)

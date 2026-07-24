package com.kara.kara_general_api.domain.model.emergency

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.RoomId
import java.time.Instant

/**
 * Alerte d'urgence déclenchée pendant une réservation : le client signale qu'un serveur doit entrer
 * dans la salle. Diffusée en temps réel aux serveurs rattachés à la réservation.
 */
data class EmergencyAlert(
    val bookingId: BookingId,
    val roomId: RoomId,
    val roomName: String,
    val message: String,
    val triggeredAt: Instant,
)

package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.RoomId
import java.time.Instant

interface BookingRepository {
    /** Persiste (upsert) la réservation et ses options sélectionnées. */
    fun save(booking: Booking): Booking

    fun findById(id: BookingId): Booking?

    /**
     * Vrai s'il existe déjà une réservation active (PENDING ou CONFIRMED) sur la salle dont le
     * créneau chevauche [startAt, endAt).
     */
    fun existsOverlapping(roomId: RoomId, startAt: Instant, endAt: Instant): Boolean

    fun updateStatus(id: BookingId, status: BookingStatus)

    /**
     * Annule (PENDING → CANCELLED) toutes les réservations dont la fenêtre de paiement est échue
     * ([expiresAt] <= [now]) et retourne le nombre de réservations affectées.
     */
    fun cancelExpiredPending(now: Instant): Int
}

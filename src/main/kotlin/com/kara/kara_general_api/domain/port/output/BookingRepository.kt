package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.UserBooking
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant

interface BookingRepository {
    /** Persiste (upsert) la réservation et ses options sélectionnées. */
    fun save(booking: Booking): Booking

    fun findById(id: BookingId): Booking?

    /**
     * Réservations (hors CANCELLED) dont la salle et le créneau chevauchent au moins un créneau d'agenda
     * du serveur [serverId]. Ordonnées par date de début croissante. Les options ne sont pas chargées.
     */
    fun findAssignedToServer(serverId: UserId): List<Booking>

    /** Toutes les réservations (supervision admin), ordonnées par date de début décroissante. */
    fun findAllBookings(): List<Booking>

    /**
     * Réservations auxquelles [userId] participe (**tous** les statuts) : celles dont il est l'organisateur
     * (`bookings.user_id`) **ou** dont il détient une part de cagnotte (`pool_shares.payer_user_id`) —
     * même sémantique d'implication que [PoolRepository.findByUserInvolvement]. Chaque réservation apparaît
     * une seule fois, quel que soit le nombre de parts détenues, et porte son rôle
     * ([UserBooking.isCreator]). Enrichies du nom et de l'adresse de la salle ainsi que des services retenus
     * (libellé + prix). Ordonnées par date de début décroissante.
     *
     * Coût fixe, indépendant du nombre de réservations : une requête pour les réservations, une seconde
     * pour les options de l'ensemble des réservations. Aucune requête supplémentaire si l'utilisateur n'a
     * aucune réservation.
     */
    fun findByUserInvolvement(userId: UserId): List<UserBooking>

    /**
     * Vrai s'il existe déjà une réservation active (PENDING ou CONFIRMED) sur la salle dont le
     * créneau chevauche [startAt, endAt).
     */
    fun existsOverlapping(
        roomId: RoomId,
        startAt: Instant,
        endAt: Instant,
    ): Boolean

    fun findNextStartAfter(
        roomId: RoomId,
        after: Instant,
        excluding: BookingId,
        now: Instant,
    ): Instant?

    fun updateStatus(
        id: BookingId,
        status: BookingStatus,
    )

    fun updateEndAt(
        id: BookingId,
        endAt: Instant,
        totalPrice: BigDecimal,
    )

    /**
     * Annule (PENDING → CANCELLED) toutes les réservations dont la fenêtre de paiement est échue
     * ([expiresAt] <= [now]) et retourne le nombre de réservations affectées.
     */
    fun cancelExpiredPending(now: Instant): Int
}

package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * Agrégat Réservation. Le prix total [totalPrice] est figé à la création par le [BookingEstimator]
 * (aucune logique de prix divergente ne vit ici). Cycle de vie : PENDING → CONFIRMED (paiement reçu)
 * ou CANCELLED. Une réservation PENDING doit être payée avant [expiresAt] (fenêtre de paiement de
 * 15 min) ; passé ce délai elle est annulée et son créneau libéré.
 */
data class Booking(
    val id: BookingId,
    val roomId: RoomId,
    val userId: UserId,
    val startAt: Instant,
    val endAt: Instant,
    val numberOfPeople: Int,
    val selectedOptionIds: List<RoomOptionId>,
    val totalPrice: BigDecimal,
    val currency: Currency,
    val status: BookingStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
) {
    fun confirm(): Booking = copy(status = BookingStatus.CONFIRMED)

    fun cancel(): Booking = copy(status = BookingStatus.CANCELLED)

    companion object {
        /** Durée de la fenêtre de paiement d'une réservation PENDING. */
        val PAYMENT_WINDOW: Duration = Duration.ofMinutes(15)

        /** Crée une réservation en attente de paiement (PENDING) avec le prix figé fourni par l'estimateur. */
        fun create(
            roomId: RoomId,
            userId: UserId,
            startAt: Instant,
            endAt: Instant,
            numberOfPeople: Int,
            selectedOptionIds: List<RoomOptionId>,
            totalPrice: BigDecimal,
            currency: Currency,
        ): Booking {
            val createdAt = Instant.now()
            return Booking(
                id = BookingId.generate(),
                roomId = roomId,
                userId = userId,
                startAt = startAt,
                endAt = endAt,
                numberOfPeople = numberOfPeople,
                selectedOptionIds = selectedOptionIds.distinct(),
                totalPrice = totalPrice,
                currency = currency,
                status = BookingStatus.PENDING,
                createdAt = createdAt,
                expiresAt = createdAt.plus(PAYMENT_WINDOW),
            )
        }
    }
}

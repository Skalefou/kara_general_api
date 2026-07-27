package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Liste les réservations dont [userId] est le propriétaire (`bookings.user_id`). */
data class ListUserBookingsCommand(
    val userId: UserId,
)

/** Service retenu par une réservation : le prix est un forfait fixe figé au catalogue. */
data class UserBookingOptionView(
    val optionId: UUID,
    val label: String,
    val price: BigDecimal,
    val currency: Currency,
)

/** Part d'un participant à la cagnotte d'une réservation (liste nominative). */
data class UserBookingPoolShareView(
    val shareId: UUID,
    val participantName: String,
    val email: String?,
    val amount: BigDecimal,
    val status: PoolShareStatus,
)

/**
 * Cagnotte incluse en ligne dans une réservation en mode SHARED_POT : évite au front une requête par
 * réservation. Absente (null) en mode PAY_ALL, où aucune cagnotte n'existe.
 */
data class UserBookingPoolView(
    val poolId: UUID,
    val status: PoolStatus,
    val targetAmount: BigDecimal,
    val collectedAmount: BigDecimal,
    val currency: Currency,
    val percentage: Int,
    val deadline: Instant,
    val shares: List<UserBookingPoolShareView>,
)

/**
 * Réservation vue par son propriétaire dans « Mes événements ». Aucun statut n'est filtré : c'est le
 * front qui étiquette et regroupe. La liste nominative des participants n'existe que via la cagnotte : en
 * mode PAY_ALL le modèle ne connaît que [numberOfPeople].
 */
data class UserBookingView(
    val bookingId: UUID,
    val roomId: UUID,
    val roomName: String,
    val roomAddress: String?,
    val startAt: Instant,
    val endAt: Instant,
    val status: BookingStatus,
    val paymentMode: PaymentMode,
    val numberOfPeople: Int,
    val totalPrice: BigDecimal,
    val currency: Currency,
    /** Échéance de la fenêtre de paiement : significative pour une réservation PENDING (15 min). */
    val expiresAt: Instant,
    val options: List<UserBookingOptionView>,
    val pool: UserBookingPoolView?,
)

sealed interface ListUserBookingsResult {
    data class Success(
        val bookings: List<UserBookingView>,
    ) : ListUserBookingsResult
}

/**
 * Réservations de l'utilisateur authentifié, tous statuts confondus, triées par date de début
 * décroissante et enrichies des services retenus et de la cagnotte éventuelle.
 */
interface ListUserBookingsUseCase {
    fun listForUser(command: ListUserBookingsCommand): ListUserBookingsResult
}

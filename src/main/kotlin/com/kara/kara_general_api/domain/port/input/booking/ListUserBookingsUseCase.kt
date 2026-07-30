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

/**
 * Liste les réservations auxquelles [userId] participe : celles dont il est l'organisateur
 * (`bookings.user_id`) **ou** dont il détient une part de cagnotte (`pool_shares.payer_user_id`).
 */
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

/**
 * Part d'un participant à la cagnotte d'une réservation. La liste est nominative **pour l'organisateur** ;
 * un participant non organisateur ne voit que ses propres parts, sans email (cf. [UserBookingPoolView]).
 */
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
 *
 * La progression ([targetAmount], [collectedAmount], [percentage]) est visible des deux rôles. [shares]
 * dépend du rôle : liste nominative complète pour l'organisateur, uniquement les parts de l'appelant (et
 * sans email) pour un participant — exactement ce que le récapitulatif de cagnotte lui montre déjà.
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
 * Réservation vue dans « Mes événements » par un utilisateur impliqué — organisateur ou détenteur d'une
 * part de cagnotte, distingués par [isCreator]. Aucun statut n'est filtré : c'est le front qui étiquette et
 * regroupe. La liste nominative des participants n'existe que via la cagnotte : en mode PAY_ALL le modèle ne
 * connaît que [numberOfPeople].
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
    /**
     * Vrai si l'utilisateur authentifié est l'organisateur de cette réservation, faux s'il n'y participe
     * qu'en détenant une part de sa cagnotte. Un participant est en **lecture seule** sur la réservation :
     * les mutations (annulation, extension, gestion de la cagnotte, commandes) restent réservées à
     * l'organisateur.
     */
    val isCreator: Boolean,
)

sealed interface ListUserBookingsResult {
    data class Success(
        val bookings: List<UserBookingView>,
    ) : ListUserBookingsResult
}

/**
 * Réservations auxquelles l'utilisateur authentifié participe — celles qu'il a créées **et** celles dont il
 * détient une part de cagnotte —, tous statuts confondus, triées par date de début décroissante et enrichies
 * des services retenus et de la cagnotte éventuelle.
 */
interface ListUserBookingsUseCase {
    fun listForUser(command: ListUserBookingsCommand): ListUserBookingsResult
}

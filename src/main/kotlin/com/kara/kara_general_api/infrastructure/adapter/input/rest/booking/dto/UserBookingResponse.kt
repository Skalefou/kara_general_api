package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.booking.UserBookingOptionView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingPoolShareView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingPoolView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingView
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Réservation de l'utilisateur authentifié (« Mes événements »). Tous les statuts sont renvoyés : le
 * front étiquette et regroupe. `pool` est non-null uniquement en mode SHARED_POT.
 */
data class UserBookingResponse(
    val bookingId: UUID,
    val roomId: UUID,
    val roomName: String,
    @field:Schema(description = "Adresse formatée de la salle (nulle si la salle est introuvable)")
    val roomAddress: String?,
    val startAt: Instant,
    val endAt: Instant,
    val status: BookingStatus,
    val paymentMode: PaymentMode,
    val numberOfPeople: Int,
    val totalPrice: BigDecimal,
    val currency: Currency,
    @field:Schema(
        description = "Échéance de la fenêtre de paiement — utile pour une réservation PENDING (15 min).",
    )
    val expiresAt: Instant,
    @field:Schema(description = "Services retenus lors de la réservation (forfaits fixes)")
    val options: List<UserBookingOptionResponse>,
    @field:Schema(
        description =
            "Cagnotte de la réservation, incluse en ligne pour éviter une requête par réservation. " +
                "Null en mode PAY_ALL : aucune cagnotte n'existe et aucun nom de participant n'est connu.",
    )
    val pool: UserBookingPoolResponse?,
) {
    companion object {
        fun from(view: UserBookingView): UserBookingResponse =
            UserBookingResponse(
                bookingId = view.bookingId,
                roomId = view.roomId,
                roomName = view.roomName,
                roomAddress = view.roomAddress,
                startAt = view.startAt,
                endAt = view.endAt,
                status = view.status,
                paymentMode = view.paymentMode,
                numberOfPeople = view.numberOfPeople,
                totalPrice = view.totalPrice,
                currency = view.currency,
                expiresAt = view.expiresAt,
                options = view.options.map { UserBookingOptionResponse.from(it) },
                pool = view.pool?.let { UserBookingPoolResponse.from(it) },
            )
    }
}

data class UserBookingOptionResponse(
    @field:Schema(description = "Identifiant du service retenu")
    val optionId: UUID,
    @field:Schema(description = "Libellé du service", example = "Ménage fin de soirée")
    val label: String,
    @field:Schema(description = "Prix forfaitaire fixe du service", example = "60.00")
    val price: BigDecimal,
    val currency: Currency,
) {
    companion object {
        fun from(view: UserBookingOptionView): UserBookingOptionResponse =
            UserBookingOptionResponse(
                optionId = view.optionId,
                label = view.label,
                price = view.price,
                currency = view.currency,
            )
    }
}

data class UserBookingPoolResponse(
    val poolId: UUID,
    val status: PoolStatus,
    val targetAmount: BigDecimal,
    @field:Schema(description = "Somme des parts autorisées ou capturées (fonds engagés)")
    val collectedAmount: BigDecimal,
    val currency: Currency,
    @field:Schema(description = "Pourcentage collecté (0-100, arrondi vers le bas)")
    val percentage: Int,
    val deadline: Instant,
    @field:Schema(description = "Liste nominative des participants et de leurs parts")
    val shares: List<UserBookingPoolShareResponse>,
) {
    companion object {
        fun from(view: UserBookingPoolView): UserBookingPoolResponse =
            UserBookingPoolResponse(
                poolId = view.poolId,
                status = view.status,
                targetAmount = view.targetAmount,
                collectedAmount = view.collectedAmount,
                currency = view.currency,
                percentage = view.percentage,
                deadline = view.deadline,
                shares = view.shares.map { UserBookingPoolShareResponse.from(it) },
            )
    }
}

data class UserBookingPoolShareResponse(
    val shareId: UUID,
    val participantName: String,
    val email: String?,
    val amount: BigDecimal,
    val status: PoolShareStatus,
) {
    companion object {
        fun from(view: UserBookingPoolShareView): UserBookingPoolShareResponse =
            UserBookingPoolShareResponse(
                shareId = view.shareId,
                participantName = view.participantName,
                email = view.email,
                amount = view.amount,
                status = view.status,
            )
    }
}

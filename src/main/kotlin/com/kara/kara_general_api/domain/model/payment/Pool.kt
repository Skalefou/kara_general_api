package com.kara.kara_general_api.domain.model.payment

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * Cagnotte (pool) partagée pour le règlement d'une réservation en mode sharedPot. Le montant cible
 * [targetAmount] est figé à la création (= prix total de la réservation). Modèle de règlement : chaque
 * part est un PaymentIntent Stripe en autorisation à capture manuelle ; **rien n'est prélevé** tant que
 * toutes les parts ne sont pas autorisées et que leur somme n'égale pas la cible. Quand la cagnotte est
 * complète, toutes les autorisations sont capturées et la réservation passe CONFIRMED. Si le délai
 * [deadline] est atteint avant complétude, toutes les autorisations sont annulées (zéro prélèvement).
 *
 * Le [deadline] est toujours strictement inférieur à 7 jours : les autorisations Stripe expirent au bout
 * d'environ 7 jours, il faut donc capturer ou annuler avant.
 */
data class Pool(
    val id: PoolId,
    val bookingId: BookingId,
    val targetAmount: BigDecimal,
    val currency: Currency,
    val status: PoolStatus,
    val deadline: Instant,
    val globalLinkToken: String,
    val createdAt: Instant,
    val extensionId: BookingExtensionId? = null,
) {
    fun isOpen(): Boolean = status == PoolStatus.OPEN

    fun isForExtension(): Boolean = extensionId != null

    fun isExpired(now: Instant): Boolean = !deadline.isAfter(now)

    fun markAuthorizedComplete(): Pool = copy(status = PoolStatus.AUTHORIZED_COMPLETE)

    fun markSettled(): Pool = copy(status = PoolStatus.SETTLED)

    fun markCancelled(): Pool = copy(status = PoolStatus.CANCELLED)

    fun markExpired(): Pool = copy(status = PoolStatus.EXPIRED)

    fun withGlobalLinkToken(token: String): Pool = copy(globalLinkToken = token)

    companion object {
        /** Fenêtre maximale d'ouverture d'une cagnotte (borne haute, < 7 jours pour les autorisations Stripe). */
        val MAX_WINDOW: Duration = Duration.ofHours(24)

        /** Marge minimale avant le début de la réservation à laquelle la cagnotte doit être réglée. */
        val PRE_RESERVATION_MARGIN: Duration = Duration.ofHours(2)

        /**
         * Délai par défaut de la cagnotte : le plus tôt entre `now + 24 h` et `début de réservation − 2 h`.
         * Garantit une échéance bien inférieure à 7 jours (validité des autorisations Stripe) tout en
         * laissant une marge avant la réservation pour capturer ou annuler.
         */
        fun defaultDeadline(
            now: Instant,
            reservationStart: Instant,
        ): Instant = minOf(now.plus(MAX_WINDOW), reservationStart.minus(PRE_RESERVATION_MARGIN))

        /**
         * Crée une cagnotte OPEN. [targetAmount] est figé (= prix total de la réservation) ; le token de lien
         * global et le délai sont fournis par l'appelant (service applicatif).
         */
        fun create(
            bookingId: BookingId,
            targetAmount: BigDecimal,
            currency: Currency,
            deadline: Instant,
            globalLinkToken: String,
            extensionId: BookingExtensionId? = null,
        ): Pool =
            Pool(
                id = PoolId.generate(),
                bookingId = bookingId,
                targetAmount = targetAmount,
                currency = currency,
                status = PoolStatus.OPEN,
                deadline = deadline,
                globalLinkToken = globalLinkToken,
                createdAt = Instant.now(),
                extensionId = extensionId,
            )
    }
}

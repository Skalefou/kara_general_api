package com.kara.kara_general_api.domain.model.invoice

import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import java.util.UUID

/**
 * Identifiant opaque d'un reçu. Il n'existe **aucune table de factures** : le reçu est une vue d'un objet
 * déjà payé. L'identifiant encode donc sa source — `PAY-<paymentId>` (paiement « payer tout ») ou
 * `SHR-<shareId>` (part de cagnotte) — ce qui le rend stable et directement résoluble.
 */
@JvmInline
value class InvoiceId(
    val value: String,
) {
    /** UUID de la source (paiement ou part), extrait du préfixe. Valide uniquement pour un id bien formé. */
    fun sourceUuid(): UUID = UUID.fromString(value.substringAfter('-'))

    companion object {
        private const val RESERVATION_PREFIX = "PAY-"
        private const val CAGNOTTE_PREFIX = "SHR-"

        fun reservation(paymentId: PaymentId): InvoiceId = InvoiceId("$RESERVATION_PREFIX${paymentId.value}")

        fun cagnotte(shareId: PoolShareId): InvoiceId = InvoiceId("$CAGNOTTE_PREFIX${shareId.value}")

        /** Résout un identifiant vers sa source, ou null s'il est inconnu / malformé (→ 404 côté REST). */
        fun parse(value: String): InvoiceSource? {
            val (isReservation, raw) =
                when {
                    value.startsWith(RESERVATION_PREFIX) -> true to value.removePrefix(RESERVATION_PREFIX)
                    value.startsWith(CAGNOTTE_PREFIX) -> false to value.removePrefix(CAGNOTTE_PREFIX)
                    else -> return null
                }
            val uuid = runCatching { UUID.fromString(raw) }.getOrNull() ?: return null
            return if (isReservation) {
                InvoiceSource.Reservation(PaymentId(uuid))
            } else {
                InvoiceSource.Cagnotte(PoolShareId(uuid))
            }
        }
    }
}

/** Source résolue d'un reçu : un paiement « payer tout » ou une part de cagnotte. */
sealed interface InvoiceSource {
    data class Reservation(
        val paymentId: PaymentId,
    ) : InvoiceSource

    data class Cagnotte(
        val shareId: PoolShareId,
    ) : InvoiceSource
}

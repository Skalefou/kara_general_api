package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.payment.PaymentId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId

/** Coordonnées de l'acheteur (client) reprises sur le reçu. */
data class InvoiceBuyer(
    val fullName: String,
    val email: String,
)

/** Reçu résolu pour téléchargement : le reçu lui-même, son propriétaire, et l'acheteur pour le PDF. */
data class InvoiceDetail(
    val invoice: Invoice,
    val ownerId: UserId,
    val buyer: InvoiceBuyer,
)

/**
 * Reçus dérivés (aucune table dédiée) : union des paiements « payer tout » PAID et des parts de cagnotte
 * CAPTURED, chaque source jointe à la réservation et à la salle (libellé + date).
 */
interface InvoiceRepository {
    /** Tous les reçus du client, triés par date d'émission décroissante. Liste vide si aucun. */
    fun findByUser(userId: UserId): List<Invoice>

    /** Reçu d'un paiement « payer tout », uniquement s'il est PAID. Null sinon (inconnu / non réglé). */
    fun findReservationDetail(paymentId: PaymentId): InvoiceDetail?

    /** Reçu d'une part de cagnotte, uniquement si elle est CAPTURED. Null sinon (inconnue / non réglée). */
    fun findCagnotteDetail(shareId: PoolShareId): InvoiceDetail?
}

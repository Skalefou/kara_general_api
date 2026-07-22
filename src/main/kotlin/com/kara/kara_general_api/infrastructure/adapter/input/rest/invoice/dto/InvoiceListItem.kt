package com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice.dto

import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Élément de la liste « Mes factures » (un reçu dérivé d'un paiement ou d'une part de cagnotte). */
data class InvoiceListItem(
    @field:Schema(description = "Identifiant opaque du reçu (PAY-<paymentId> ou SHR-<shareId>)", example = "PAY-3f7a...")
    val invoiceId: String,
    @field:Schema(description = "Numéro de reçu lisible et déterministe", example = "INV-2026-3F7Q2K9A")
    val number: String,
    val type: InvoiceType,
    @field:Schema(description = "Libellé du reçu : nom de la salle réservée", example = "Salle Étoile")
    val label: String,
    @field:Schema(description = "Montant TTC réglé pour cette source", example = "435.00")
    val amount: BigDecimal,
    val currency: Currency,
    @field:Schema(description = "Date d'émission (= création de la source), ISO 8601 UTC")
    val issuedAt: Instant,
    val bookingId: UUID,
) {
    companion object {
        fun from(invoice: Invoice): InvoiceListItem =
            InvoiceListItem(
                invoiceId = invoice.id.value,
                number = invoice.number(),
                type = invoice.type,
                label = invoice.label,
                amount = invoice.amount,
                currency = invoice.currency,
                issuedAt = invoice.issuedAt,
                bookingId = invoice.bookingId.value,
            )
    }
}

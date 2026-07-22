package com.kara.kara_general_api.domain.model.invoice

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

/**
 * Reçu dérivé (aucune persistance dédiée) d'un objet déjà payé. Son [number] humain est calculé **de façon
 * déterministe** à partir de l'identifiant de la source (même approche que le code de billet) : aucune
 * séquence, aucune colonne. Format : `INV-<année>-XXXXXXXX` où les 8 caractères encodent les 40 bits de
 * poids fort de l'UUID source en base 32 « Crockford ».
 */
data class Invoice(
    val id: InvoiceId,
    val type: InvoiceType,
    /** Libellé lisible : le nom de la salle réservée. */
    val label: String,
    /** Montant TTC (le reçu porte le montant réglé pour cette source). */
    val amount: BigDecimal,
    val currency: Currency,
    /** Date d'émission = date de création de la source (paiement / part). */
    val issuedAt: Instant,
    val bookingId: BookingId,
) {
    /** Numéro de reçu lisible, déterministe et stable pour une source donnée. */
    fun number(): String {
        val year = issuedAt.atZone(ZoneOffset.UTC).year
        var value = id.sourceUuid().mostSignificantBits ushr 24 // 40 bits de poids fort
        val chars = CharArray(8)
        for (i in 7 downTo 0) {
            chars[i] = CROCKFORD_ALPHABET[(value and 0x1FL).toInt()]
            value = value ushr 5
        }
        return "INV-$year-${String(chars)}"
    }

    private companion object {
        const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    }
}

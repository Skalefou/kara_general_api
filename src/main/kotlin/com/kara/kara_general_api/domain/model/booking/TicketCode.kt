package com.kara.kara_general_api.domain.model.booking

/**
 * Code de billet lisible dérivé **de façon déterministe** de l'identifiant de la réservation (aucune
 * colonne ni migration : le même booking produit toujours le même code). Format : `KARA-TKT-XXXXXXXX`
 * où les 8 caractères encodent les 40 bits de poids fort de l'UUID en base 32 « Crockford » (sans I, L,
 * O, U pour éviter les confusions). Le front y rend le QR code.
 */

private const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
private const val TICKET_PREFIX = "KARA-TKT-"

/** Code de billet déterministe de la réservation (toujours calculable, indépendant du statut). */
fun Booking.ticketCode(): String {
    var value = id.value.mostSignificantBits ushr 24 // 40 bits de poids fort
    val chars = CharArray(8)
    for (i in 7 downTo 0) {
        chars[i] = CROCKFORD_ALPHABET[(value and 0x1FL).toInt()]
        value = value ushr 5
    }
    return TICKET_PREFIX + String(chars)
}

/** Code de billet exposé au client : présent uniquement lorsque la réservation est CONFIRMED, sinon null. */
fun Booking.ticketCodeOrNull(): String? = if (status == BookingStatus.CONFIRMED) ticketCode() else null

package com.kara.kara_general_api.domain.model.invoice

/**
 * Nature d'un reçu (« facture » au sens MVP « Mes factures » : un reçu, pas une facture légale française).
 *
 * - [RESERVATION] : dérivé d'un paiement « payer tout » capturé (PAID).
 * - [CAGNOTTE] : dérivé d'une part de cagnotte capturée (CAPTURED).
 */
enum class InvoiceType {
    RESERVATION,
    CAGNOTTE,
}

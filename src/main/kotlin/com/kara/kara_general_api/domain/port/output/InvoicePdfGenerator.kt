package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.invoice.Invoice

/**
 * Port secondaire de génération du PDF d'un reçu. L'implémentation (mise en page, en-tête vendeur) reste
 * dans l'adaptateur ; le domaine ne fournit que le reçu et l'acheteur.
 */
interface InvoicePdfGenerator {
    /** Produit les octets du PDF du [invoice] pour l'[buyer]. */
    fun generate(invoice: Invoice, buyer: InvoiceBuyer): ByteArray
}

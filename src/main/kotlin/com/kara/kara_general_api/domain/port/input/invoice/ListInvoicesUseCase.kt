package com.kara.kara_general_api.domain.port.input.invoice

import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.user.UserId

/** Liste les reçus d'un client : union de ses paiements « payer tout » et de ses parts de cagnotte réglées. */
interface ListInvoicesUseCase {
    fun listInvoices(userId: UserId): List<Invoice>
}

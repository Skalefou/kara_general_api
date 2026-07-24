package com.kara.kara_general_api.domain.port.input.invoice

import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.user.UserId

sealed interface GetInvoiceDownloadResult {
    /** URL signée courte durée vers le PDF du reçu (généré paresseusement s'il n'existe pas encore). */
    data class Found(val downloadUrl: String) : GetInvoiceDownloadResult

    /** Identifiant inconnu, ou source non réglée (paiement non PAID / part non CAPTURED). */
    data object NotFound : GetInvoiceDownloadResult

    /** La source existe et est réglée mais appartient à un autre utilisateur. */
    data object NotOwner : GetInvoiceDownloadResult
}

interface GetInvoiceDownloadUseCase {
    fun getDownloadUrl(invoiceId: InvoiceId, requesterId: UserId): GetInvoiceDownloadResult
}

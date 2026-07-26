package com.kara.kara_general_api.application.service.invoice

import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.invoice.InvoiceSource
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadResult
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.InvoiceDetail
import com.kara.kara_general_api.domain.port.output.InvoicePdfGenerator
import com.kara.kara_general_api.domain.port.output.InvoiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * Résout un reçu vers une URL de téléchargement signée. Le PDF est généré **paresseusement** : à la
 * première demande, si l'objet `invoices/<invoiceId>.pdf` est absent du bucket privé, il est généré puis
 * téléversé (écrasement idempotent), avant de retourner une URL signée de courte durée. Aucun hook au
 * moment de la capture : rien n'est produit tant que le client ne demande pas son reçu.
 */
@Service
class GetInvoiceDownloadService(
    private val invoiceRepository: InvoiceRepository,
    private val pdfGenerator: InvoicePdfGenerator,
    private val objectStorage: ImageStoragePort,
) : GetInvoiceDownloadUseCase {
    @Transactional(readOnly = true)
    override fun getDownloadUrl(
        invoiceId: InvoiceId,
        requesterId: UserId,
    ): GetInvoiceDownloadResult {
        val detail = resolve(invoiceId) ?: return GetInvoiceDownloadResult.NotFound
        if (detail.ownerId != requesterId) return GetInvoiceDownloadResult.NotOwner

        val key = "$INVOICE_KEY_PREFIX${invoiceId.value}.pdf"
        if (!objectStorage.exists(ImageVisibility.PRIVATE, key)) {
            val pdf = pdfGenerator.generate(detail.invoice, detail.buyer)
            objectStorage.upload(ImageVisibility.PRIVATE, key, pdf, "application/pdf")
        }
        return GetInvoiceDownloadResult.Found(objectStorage.signedUrl(key, SIGNED_URL_TTL))
    }

    private fun resolve(invoiceId: InvoiceId): InvoiceDetail? =
        when (val source = InvoiceId.parse(invoiceId.value)) {
            is InvoiceSource.Reservation -> invoiceRepository.findReservationDetail(source.paymentId)
            is InvoiceSource.Cagnotte -> invoiceRepository.findCagnotteDetail(source.shareId)
            null -> null
        }

    private companion object {
        const val INVOICE_KEY_PREFIX = "invoices/"
        val SIGNED_URL_TTL: Duration = Duration.ofMinutes(15)
    }
}

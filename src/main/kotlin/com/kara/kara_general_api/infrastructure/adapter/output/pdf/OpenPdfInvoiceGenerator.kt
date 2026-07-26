package com.kara.kara_general_api.infrastructure.adapter.output.pdf

import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.invoice.InvoiceType
import com.kara.kara_general_api.domain.port.output.InvoiceBuyer
import com.kara.kara_general_api.domain.port.output.InvoicePdfGenerator
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Génère le PDF d'un reçu par mise en page programmatique (OpenPDF, sans moteur de template). C'est un reçu,
 * pas une facture légale française : en-tête vendeur (constantes de config), acheteur, numéro de reçu
 * déterministe, date, une ligne (salle + montant TTC). Aucun bloc TVA/mentions légales.
 */
@Component
class OpenPdfInvoiceGenerator(
    @Value("\${kara.invoice.seller.name:Kara SAS}") private val sellerName: String,
    @Value("\${kara.invoice.seller.address:1 rue de la Fête, 75001 Paris, France}") private val sellerAddress: String,
    @Value("\${kara.invoice.seller.email:facturation@kara.example}") private val sellerEmail: String,
) : InvoicePdfGenerator {
    override fun generate(
        invoice: Invoice,
        buyer: InvoiceBuyer,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val document = Document()
        PdfWriter.getInstance(document, output)
        document.open()

        document.add(Paragraph(sellerName, TITLE_FONT))
        document.add(Paragraph(sellerAddress, NORMAL_FONT))
        document.add(Paragraph(sellerEmail, NORMAL_FONT))
        document.add(spacer())

        val heading = Paragraph("Reçu ${invoice.number()}", HEADING_FONT)
        document.add(heading)
        document.add(Paragraph("Date : ${DATE_FORMATTER.format(invoice.issuedAt.atZone(ZoneOffset.UTC))}", NORMAL_FONT))
        document.add(Paragraph("Type : ${typeLabel(invoice.type)}", NORMAL_FONT))
        document.add(spacer())

        document.add(Paragraph("Facturé à", HEADING_FONT))
        document.add(Paragraph(buyer.fullName, NORMAL_FONT))
        document.add(Paragraph(buyer.email, NORMAL_FONT))
        document.add(spacer())

        document.add(Paragraph("Détail", HEADING_FONT))
        document.add(Paragraph(invoice.label, NORMAL_FONT))
        val total = Paragraph("Total TTC : ${formatAmount(invoice)} ${invoice.currency.name}", HEADING_FONT)
        total.alignment = Element.ALIGN_RIGHT
        document.add(total)

        document.close()
        return output.toByteArray()
    }

    private fun spacer() = Paragraph(" ", NORMAL_FONT)

    private fun typeLabel(type: InvoiceType): String =
        when (type) {
            InvoiceType.RESERVATION -> "Réservation"
            InvoiceType.CAGNOTTE -> "Cagnotte (part)"
        }

    private fun formatAmount(invoice: Invoice): String = String.format(Locale.FRANCE, "%.2f", invoice.amount)

    private companion object {
        val TITLE_FONT: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
        val HEADING_FONT: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)
        val NORMAL_FONT: Font = FontFactory.getFont(FontFactory.HELVETICA, 10f)
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE)
    }
}

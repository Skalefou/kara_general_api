package com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice

import com.kara.kara_general_api.domain.model.invoice.InvoiceId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadResult
import com.kara.kara_general_api.domain.port.input.invoice.GetInvoiceDownloadUseCase
import com.kara.kara_general_api.domain.port.input.invoice.ListInvoicesUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice.dto.InvoiceDownloadResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice.dto.InvoiceListItem
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/invoices")
class InvoiceController(
    private val listInvoicesUseCase: ListInvoicesUseCase,
    private val getInvoiceDownloadUseCase: GetInvoiceDownloadUseCase,
) : InvoiceApi {

    override fun listInvoices(authentication: Authentication): ResponseEntity<Any> {
        val userId = UserId(UUID.fromString(authentication.name))
        val items = listInvoicesUseCase.listInvoices(userId).map(InvoiceListItem::from)
        return ResponseEntity.ok(items)
    }

    override fun downloadInvoice(
        invoiceId: String,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val requesterId = UserId(UUID.fromString(authentication.name))
        return when (val result = getInvoiceDownloadUseCase.getDownloadUrl(InvoiceId(invoiceId), requesterId)) {
            is GetInvoiceDownloadResult.Found ->
                ResponseEntity.ok(InvoiceDownloadResponse(result.downloadUrl))
            GetInvoiceDownloadResult.NotOwner -> invoiceNotOwner()
            GetInvoiceDownloadResult.NotFound -> invoiceNotFound()
        }
    }

    private fun invoiceNotOwner(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Ce reçu n'appartient pas à l'utilisateur courant.",
            ).apply {
                title = "Accès refusé"
                setProperty("code", "INVOICE_NOT_OWNER")
            },
        )

    private fun invoiceNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Aucun reçu ne correspond à cet identifiant.",
            ).apply {
                title = "Reçu introuvable"
                setProperty("code", "INVOICE_NOT_FOUND")
            },
        )
}

package com.kara.kara_general_api.application.service.invoice

import com.kara.kara_general_api.domain.model.invoice.Invoice
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.invoice.ListInvoicesUseCase
import com.kara.kara_general_api.domain.port.output.InvoiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Liste les reçus du client (union paiements PAID + parts CAPTURED). Lecture seule. */
@Service
class ListInvoicesService(
    private val invoiceRepository: InvoiceRepository,
) : ListInvoicesUseCase {

    @Transactional(readOnly = true)
    override fun listInvoices(userId: UserId): List<Invoice> = invoiceRepository.findByUser(userId)
}

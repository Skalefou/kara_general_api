package com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice

import com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice.dto.InvoiceDownloadResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice.dto.InvoiceListItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Factures", description = "Reçus du client (« Mes factures »)")
interface InvoiceApi {
    @Operation(
        summary = "Lister mes factures",
        description =
            "Liste les reçus du client authentifié : union de ses paiements « payer tout » réglés et de " +
                "ses parts de cagnotte capturées, triée par date d'émission décroissante. Tableau vide si aucun reçu.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des reçus",
                content = [Content(array = ArraySchema(schema = Schema(implementation = InvoiceListItem::class)))],
            ),
        ],
    )
    @GetMapping
    fun listInvoices(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Télécharger une facture",
        description =
            "Retourne une URL signée courte durée vers le PDF du reçu. Le PDF est généré " +
                "paresseusement à la première demande. Réservé au propriétaire du paiement / de la part.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "URL de téléchargement signée",
                content = [Content(schema = Schema(implementation = InvoiceDownloadResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Le reçu appartient à un autre utilisateur (INVOICE_NOT_OWNER)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Reçu inconnu ou source non réglée (INVOICE_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @GetMapping("/{invoiceId}")
    fun downloadInvoice(
        @PathVariable invoiceId: String,
        authentication: Authentication,
    ): ResponseEntity<Any>
}

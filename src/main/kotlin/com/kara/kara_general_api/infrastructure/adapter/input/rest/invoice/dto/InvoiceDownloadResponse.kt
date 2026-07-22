package com.kara.kara_general_api.infrastructure.adapter.input.rest.invoice.dto

import io.swagger.v3.oas.annotations.media.Schema

/** Réponse du téléchargement d'un reçu : URL signée courte durée vers le PDF (le front la suit directement). */
data class InvoiceDownloadResponse(
    @field:Schema(description = "URL signée courte durée vers le PDF du reçu")
    val downloadUrl: String,
)

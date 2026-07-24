package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto

import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * Secrets du PaymentSheet Stripe pour autoriser (bloquer) une part de cagnotte. La carte n'est PAS
 * débitée : capture manuelle, différée jusqu'à complétude de la cagnotte.
 */
data class AuthorizePoolShareResponse(
    @field:Schema(description = "Identifiant de la part réglée")
    val shareId: UUID,
    val clientSecret: String,
    val ephemeralKeySecret: String,
    val customerId: String,
    val publishableKey: String,
) {
    companion object {
        fun from(ready: AuthorizePoolShareResult.Ready): AuthorizePoolShareResponse =
            AuthorizePoolShareResponse(
                shareId = ready.shareId,
                clientSecret = ready.clientSecret,
                ephemeralKeySecret = ready.ephemeralKeySecret,
                customerId = ready.customerId,
                publishableKey = ready.publishableKey,
            )
    }
}

/** Nouveau token de lien global après régénération. */
data class RegeneratePoolLinkResponse(
    val globalLinkToken: String,
)

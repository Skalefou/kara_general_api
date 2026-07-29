package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto

import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkResult
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

/** Nouveau token de lien global après régénération, et le lien de partage complet correspondant. */
data class RegeneratePoolLinkResponse(
    val globalLinkToken: String,
    @field:Schema(
        description = "Lien de partage global régénéré, prêt à l'emploi (construit par le serveur)",
        example = "https://link.karapi.fr/join/8f14e45fceea167a5a36dedd4bea2543",
    )
    val globalShareUrl: String,
) {
    companion object {
        fun from(regenerated: RegeneratePoolLinkResult.Regenerated): RegeneratePoolLinkResponse =
            RegeneratePoolLinkResponse(
                globalLinkToken = regenerated.globalLinkToken,
                globalShareUrl = regenerated.globalShareUrl,
            )
    }
}

package com.kara.kara_general_api.domain.port.input.payment

data class StripeWebhookCommand(
    val payload: String,
    val signature: String?,
)

sealed interface StripeWebhookResult {
    /** Événement traité (paiement marqué PAID + réservation CONFIRMED) ou déjà à jour (idempotent). */
    data object Handled : StripeWebhookResult

    /** Événement reconnu mais ignoré (type non géré, paiement inconnu). */
    data object Ignored : StripeWebhookResult

    /** Signature Stripe absente ou invalide. */
    data object InvalidSignature : StripeWebhookResult
}

interface HandleStripeWebhookUseCase {
    fun handle(command: StripeWebhookCommand): StripeWebhookResult
}

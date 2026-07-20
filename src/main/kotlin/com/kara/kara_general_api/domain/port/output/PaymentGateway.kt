package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.User
import java.math.BigDecimal

/** Résultat de création d'un PaymentIntent Stripe (secrets destinés au PaymentSheet côté front). */
data class PaymentIntentResult(
    val clientSecret: String,
    val paymentIntentId: String,
)

/**
 * Événement Stripe vérifié et décodé, réduit aux informations utiles au domaine (aucun type Stripe
 * ne franchit la frontière de l'hexagone).
 */
data class StripeWebhookEvent(
    val type: String,
    val paymentIntentId: String?,
)

/** Port secondaire vers Stripe (paiement « payer tout » façon PaymentSheet). */
interface PaymentGateway {
    /**
     * Retourne l'identifiant client Stripe de l'utilisateur, en le créant paresseusement s'il n'existe
     * pas encore. L'appelant (service applicatif) est responsable de la persistance de l'identifiant.
     */
    fun ensureCustomer(user: User): String

    /** Crée une clé éphémère pour le client et retourne son secret (requis par le PaymentSheet). */
    fun createEphemeralKey(customerId: String): String

    /** Crée un PaymentIntent rattaché au client Stripe et retourne son client secret + identifiant. */
    fun createPaymentIntent(amount: BigDecimal, currency: Currency, customerId: String): PaymentIntentResult

    /**
     * Crée un PaymentIntent en **capture manuelle** (autorisation seule, aucun prélèvement) pour une part de
     * cagnotte. Les fonds sont bloqués à la confirmation côté front (événement `amount_capturable_updated`)
     * puis capturés seulement quand toute la cagnotte est complète.
     */
    fun createManualCapturePaymentIntent(
        amount: BigDecimal,
        currency: Currency,
        customerId: String,
    ): PaymentIntentResult

    /** Capture une autorisation existante (prélèvement effectif). Idempotent côté Stripe si déjà capturée. */
    fun capturePaymentIntent(paymentIntentId: String)

    /** Annule une autorisation existante (libère le blocage, zéro prélèvement). */
    fun cancelPaymentIntent(paymentIntentId: String)

    /** Vérifie la signature du webhook et décode l'événement. Retourne null si la signature est invalide. */
    fun verifyAndParseWebhook(payload: String, signature: String): StripeWebhookEvent?

    /** Clé publiable Stripe à transmettre au front pour initialiser le PaymentSheet. */
    fun publishableKey(): String
}

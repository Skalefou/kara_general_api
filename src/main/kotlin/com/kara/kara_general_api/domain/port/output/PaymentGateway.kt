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

/**
 * Statut d'un PaymentIntent chez la passerelle, réduit à ce dont l'application a besoin. Le domaine
 * ne connaît que cet énuméré : aucune chaîne brute de la passerelle ne circule au-delà de l'adaptateur.
 */
enum class PaymentIntentStatus {
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_CONFIRMATION,
    REQUIRES_ACTION,
    PROCESSING,
    REQUIRES_CAPTURE,
    SUCCEEDED,
    CANCELED,

    /** Statut non reconnu (passerelle en avance sur le contrat) : traité comme non exploitable. */
    UNKNOWN,
    ;

    /** Intent encore payable en l'état : son client secret peut être re-servi au front. */
    fun isReusableForPayment(): Boolean = this == REQUIRES_PAYMENT_METHOD || this == REQUIRES_CONFIRMATION

    companion object {
        /** Mappe le statut brut de la passerelle (`requires_payment_method`, `succeeded`, …). */
        fun from(raw: String?): PaymentIntentStatus = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

/** Instantané d'un PaymentIntent relu chez la passerelle (statut réel + client secret réutilisable). */
data class PaymentIntentSnapshot(
    val paymentIntentId: String,
    val status: PaymentIntentStatus,
    val clientSecret: String?,
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

    /**
     * Crée un PaymentIntent rattaché au client Stripe et retourne son client secret + identifiant.
     *
     * [idempotencyKey] : clé d'idempotence transmise à la passerelle. Deux appels successifs avec la même
     * clé retournent le **même** intent au lieu d'en créer deux (double appui sur « Payer »).
     */
    fun createPaymentIntent(
        amount: BigDecimal,
        currency: Currency,
        customerId: String,
        idempotencyKey: String? = null,
    ): PaymentIntentResult

    /**
     * Relit un PaymentIntent existant chez la passerelle (statut réel + client secret). Retourne null si
     * l'intent est introuvable ou si la passerelle est injoignable. Sert à la réconciliation d'un paiement
     * dont le webhook n'est jamais arrivé, et à la réutilisation d'un intent encore payable.
     */
    fun retrievePaymentIntent(paymentIntentId: String): PaymentIntentSnapshot?

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

    /**
     * Capture une autorisation existante (prélèvement effectif), **à hauteur exacte de [amount]**.
     *
     * Le montant est explicite et jamais implicite : une autorisation à capture manuelle est figée sur le
     * montant demandé à sa création, alors que le montant dû par une part de cagnotte peut avoir été revu à la
     * baisse entre-temps (découpe du reliquat du créateur). Capturer sans montant prélèverait l'intégralité de
     * l'autorisation, donc plus que le dû. Avec [amount], le surplus autorisé est libéré au lieu d'être
     * prélevé. Idempotent côté Stripe si déjà capturée.
     */
    fun capturePaymentIntent(
        paymentIntentId: String,
        amount: BigDecimal,
    )

    /** Annule une autorisation existante (libère le blocage, zéro prélèvement). */
    fun cancelPaymentIntent(paymentIntentId: String)

    /** Rembourse intégralement un paiement déjà capturé (PaymentIntent). Utilisé à l'annulation d'une
     *  réservation confirmée (payer tout ou cagnotte réglée). */
    fun refundPaymentIntent(paymentIntentId: String)

    /** Vérifie la signature du webhook et décode l'événement. Retourne null si la signature est invalide. */
    fun verifyAndParseWebhook(
        payload: String,
        signature: String,
    ): StripeWebhookEvent?

    /** Clé publiable Stripe à transmettre au front pour initialiser le PaymentSheet. */
    fun publishableKey(): String
}

package com.kara.kara_general_api.infrastructure.adapter.output.payment

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.kara.kara_general_api.domain.port.output.PaymentIntentSnapshot
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.StripeWebhookEvent
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import com.stripe.net.RequestOptions
import com.stripe.net.Webhook
import com.stripe.param.CustomerCreateParams
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentIntentCaptureParams
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.RefundCreateParams
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Adaptateur secondaire Stripe (paiement « payer tout » façon PaymentSheet). Utilise l'API statique
 * Stripe, dont la clé est fixée par [com.kara.kara_general_api.infrastructure.config.StripeConfig].
 * Désactivé sur le profil "test" (les tests mockent le port).
 */
@Component
@Profile("!test")
class StripePaymentGatewayAdapter(
    @Value("\${kara.stripe.publishable-key}") private val publishableKey: String,
    @Value("\${kara.stripe.webhook-secret}") private val webhookSecret: String,
    // Version d'API Stripe attendue par le SDK MOBILE (flutter_stripe), et NON celle de stripe-java côté
    // serveur : la clé éphémère doit être scellée sur cette version, sinon la PaymentSheet la rejette.
    @Value("\${kara.stripe.mobile-api-version}") private val mobileApiVersion: String,
) : PaymentGateway {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun ensureCustomer(user: User): String {
        user.stripeCustomerId?.let { return it }
        val params =
            CustomerCreateParams
                .builder()
                .setEmail(user.email.value)
                .setName("${user.firstName} ${user.lastName}")
                .build()
        return Customer.create(params).id
    }

    override fun createEphemeralKey(customerId: String): String {
        // La version d'API doit être celle du SDK mobile (flutter_stripe) et posée DIRECTEMENT sur les params :
        // EphemeralKey.create valide params.getStripeVersion() et ignore RequestOptions.unsafeSetStripeVersionOverride.
        val params =
            EphemeralKeyCreateParams
                .builder()
                .setCustomer(customerId)
                .setStripeVersion(mobileApiVersion)
                .build()
        return EphemeralKey.create(params).secret
    }

    override fun createPaymentIntent(
        amount: BigDecimal,
        currency: Currency,
        customerId: String,
        idempotencyKey: String?,
    ): PaymentIntentResult {
        val params =
            basePaymentIntentParams(amount, currency, customerId).build()
        // Clé d'idempotence : Stripe rejoue le même intent (au lieu d'en créer un second) si l'appelant
        // réémet la même demande, ce qui évite deux intents payables pour la même réservation.
        val options =
            RequestOptions
                .builder()
                .setIdempotencyKey(idempotencyKey)
                .build()
        val intent = PaymentIntent.create(params, options)
        return PaymentIntentResult(clientSecret = intent.clientSecret, paymentIntentId = intent.id)
    }

    override fun retrievePaymentIntent(paymentIntentId: String): PaymentIntentSnapshot? =
        runCatching { PaymentIntent.retrieve(paymentIntentId) }
            .onFailure { logger.warn("Failed to retrieve a Stripe payment intent", it) }
            .getOrNull()
            ?.let { intent ->
                PaymentIntentSnapshot(
                    paymentIntentId = intent.id,
                    status = PaymentIntentStatus.from(intent.status),
                    clientSecret = intent.clientSecret,
                )
            }

    override fun createManualCapturePaymentIntent(
        amount: BigDecimal,
        currency: Currency,
        customerId: String,
    ): PaymentIntentResult {
        // Capture manuelle : les fonds sont seulement AUTORISÉS (bloqués), jamais prélevés ici. La capture
        // n'a lieu que lorsque toute la cagnotte est complète (cf. PoolSettlementService).
        val params =
            basePaymentIntentParams(amount, currency, customerId)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .build()
        val intent = PaymentIntent.create(params)
        return PaymentIntentResult(clientSecret = intent.clientSecret, paymentIntentId = intent.id)
    }

    override fun capturePaymentIntent(
        paymentIntentId: String,
        amount: BigDecimal,
    ) {
        // `amount_to_capture` : on ne prélève JAMAIS plus que le montant réellement dû. `capture()` sans
        // paramètre prélèverait l'intégralité du montant autorisé, qui peut être supérieur au dû si le montant
        // de la part a été revu à la baisse après l'autorisation. Stripe libère le surplus non capturé.
        val params =
            PaymentIntentCaptureParams
                .builder()
                .setAmountToCapture(toMinorUnits(amount))
                .build()
        PaymentIntent.retrieve(paymentIntentId).capture(params)
    }

    override fun cancelPaymentIntent(paymentIntentId: String) {
        PaymentIntent.retrieve(paymentIntentId).cancel()
    }

    override fun refundPaymentIntent(paymentIntentId: String) {
        // Remboursement intégral d'un paiement capturé (annulation d'une réservation confirmée).
        Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentIntentId).build())
    }

    private fun basePaymentIntentParams(
        amount: BigDecimal,
        currency: Currency,
        customerId: String,
    ): PaymentIntentCreateParams.Builder {
        return PaymentIntentCreateParams
            .builder()
            .setAmount(toMinorUnits(amount))
            .setCurrency(currency.name.lowercase())
            .setCustomer(customerId)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods
                    .builder()
                    .setEnabled(true)
                    .build(),
            )
    }

    /** Conversion unique montant décimal -> plus petite unité monétaire, partagée par la création et la
     *  capture d'un PaymentIntent : les deux doivent arrondir à l'identique. */
    private fun toMinorUnits(amount: BigDecimal): Long = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()

    override fun verifyAndParseWebhook(
        payload: String,
        signature: String,
    ): StripeWebhookEvent? =
        try {
            val event = Webhook.constructEvent(payload, signature, webhookSecret)
            StripeWebhookEvent(type = event.type, paymentIntentId = extractPaymentIntentId(event))
        } catch (_: SignatureVerificationException) {
            logger.warn("Stripe webhook signature verification failed")
            null
        }

    override fun publishableKey(): String = publishableKey

    private fun extractPaymentIntentId(event: Event): String? {
        val deserializer = event.dataObjectDeserializer
        val stripeObject =
            if (deserializer.getObject().isPresent) {
                deserializer.getObject().get()
            } else {
                // La désérialisation échoue typiquement quand la version d'API de l'événement diffère de
                // celle du SDK : sans ce log, l'événement était silencieusement ignoré (réponse 200).
                runCatching { deserializer.deserializeUnsafe() }
                    .onFailure { logger.warn("Failed to deserialize the data object of Stripe event type={}", event.type, it) }
                    .getOrNull()
            }
        val paymentIntentId = (stripeObject as? PaymentIntent)?.id
        if (paymentIntentId == null) {
            logger.warn("No payment intent id could be extracted from Stripe event type={}", event.type)
        }
        return paymentIntentId
    }
}

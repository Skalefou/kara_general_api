package com.kara.kara_general_api.infrastructure.adapter.output.payment

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.kara.kara_general_api.domain.port.output.StripeWebhookEvent
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import com.stripe.param.CustomerCreateParams
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentIntentCreateParams
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

    override fun ensureCustomer(user: User): String {
        user.stripeCustomerId?.let { return it }
        val params =
            CustomerCreateParams.builder()
                .setEmail(user.email.value)
                .setName("${user.firstName} ${user.lastName}")
                .build()
        return Customer.create(params).id
    }

    override fun createEphemeralKey(customerId: String): String {
        // La version d'API doit être celle du SDK mobile (flutter_stripe) et posée DIRECTEMENT sur les params :
        // EphemeralKey.create valide params.getStripeVersion() et ignore RequestOptions.unsafeSetStripeVersionOverride.
        val params =
            EphemeralKeyCreateParams.builder()
                .setCustomer(customerId)
                .setStripeVersion(mobileApiVersion)
                .build()
        return EphemeralKey.create(params).secret
    }

    override fun createPaymentIntent(amount: BigDecimal, currency: Currency, customerId: String): PaymentIntentResult {
        val params =
            basePaymentIntentParams(amount, currency, customerId).build()
        val intent = PaymentIntent.create(params)
        return PaymentIntentResult(clientSecret = intent.clientSecret, paymentIntentId = intent.id)
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

    override fun capturePaymentIntent(paymentIntentId: String) {
        PaymentIntent.retrieve(paymentIntentId).capture()
    }

    override fun cancelPaymentIntent(paymentIntentId: String) {
        PaymentIntent.retrieve(paymentIntentId).cancel()
    }

    private fun basePaymentIntentParams(
        amount: BigDecimal,
        currency: Currency,
        customerId: String,
    ): PaymentIntentCreateParams.Builder {
        val amountMinorUnits = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        return PaymentIntentCreateParams.builder()
            .setAmount(amountMinorUnits)
            .setCurrency(currency.name.lowercase())
            .setCustomer(customerId)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build(),
            )
    }

    override fun verifyAndParseWebhook(payload: String, signature: String): StripeWebhookEvent? =
        try {
            val event = Webhook.constructEvent(payload, signature, webhookSecret)
            StripeWebhookEvent(type = event.type, paymentIntentId = extractPaymentIntentId(event))
        } catch (_: SignatureVerificationException) {
            null
        }

    override fun publishableKey(): String = publishableKey

    private fun extractPaymentIntentId(event: Event): String? {
        val deserializer = event.dataObjectDeserializer
        val stripeObject =
            if (deserializer.getObject().isPresent) {
                deserializer.getObject().get()
            } else {
                runCatching { deserializer.deserializeUnsafe() }.getOrNull()
            }
        return (stripeObject as? PaymentIntent)?.id
    }
}
